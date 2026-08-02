package com.jinfu.flowable.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jinfu.common.exception.BusinessException;
import com.jinfu.common.result.ResultCode;
import com.jinfu.flowable.dto.*;
import com.jinfu.flowable.service.FlowProcessService;
import com.jinfu.security.entity.LoginUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.UserTask;
import org.flowable.bpmn.converter.BpmnXMLConverter;
import org.flowable.engine.*;
import org.flowable.engine.history.HistoricActivityInstance;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.history.HistoricProcessInstanceQuery;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.task.Comment;
import org.flowable.task.api.Task;
import org.flowable.task.api.TaskQuery;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FlowProcessServiceImpl implements FlowProcessService {

    private final RepositoryService repositoryService;
    private final RuntimeService runtimeService;
    private final TaskService taskService;
    private final HistoryService historyService;

    // ==================== Process Definition ====================

    @Override
    public IPage<ProcessDefinition> getProcessDefinitions(Page<ProcessDefinition> page, String keyword) {
        var query = repositoryService.createProcessDefinitionQuery()
                .latestVersion()
                .orderByProcessDefinitionName().asc();

        if (keyword != null && !keyword.isEmpty()) {
            query.processDefinitionNameLike("%" + keyword + "%");
        }

        long total = query.count();
        List<ProcessDefinition> records = query.listPage((int) page.offset(), (int) page.getSize());

        page.setTotal(total);
        page.setRecords(records);
        return page;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deployProcessDefinition(String name, String bpmnXml) {
        try {
            repositoryService.createDeployment()
                    .name(name)
                    .addString(name + ".bpmn20.xml", bpmnXml)
                    .deploy();
            log.info("Deployed process definition: {}", name);
        } catch (Exception e) {
            log.error("Failed to deploy process definition: {}", name, e);
            throw new BusinessException(ResultCode.DEPLOY_FAILED, e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteDeployment(String deploymentId) {
        repositoryService.deleteDeployment(deploymentId, true);
        log.info("Deleted deployment: {}", deploymentId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void suspendDefinition(String procDefId) {
        repositoryService.suspendProcessDefinitionById(procDefId, true, null);
        log.info("Suspended process definition: {}", procDefId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void activateDefinition(String procDefId) {
        repositoryService.activateProcessDefinitionById(procDefId, true, null);
        log.info("Activated process definition: {}", procDefId);
    }

    // ==================== Process Instance ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String startProcess(String procDefKey, String businessKey, Map<String, Object> variables) {
        LoginUser loginUser = getLoginUser();

        if (variables == null) {
            variables = new HashMap<>();
        }
        variables.put("starter", loginUser.getUsername());
        variables.put("starterId", loginUser.getUserId());

        var processInstance = runtimeService.startProcessInstanceByKey(
                procDefKey, businessKey, variables);

        log.info("Started process: key={}, instanceId={}, starter={}",
                procDefKey, processInstance.getId(), loginUser.getUsername());

        return processInstance.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelProcess(String procInstId, String reason) {
        runtimeService.deleteProcessInstance(procInstId, reason);
        log.info("Cancelled process instance: {}, reason: {}", procInstId, reason);
    }

    @Override
    public IPage<HistoricProcessInstance> getAppliedProcesses(
            Page<HistoricProcessInstance> page, String keyword) {
        LoginUser loginUser = getLoginUser();

        HistoricProcessInstanceQuery query = historyService.createHistoricProcessInstanceQuery()
                .startedBy(loginUser.getUsername())
                .orderByProcessInstanceStartTime().desc();

        long total = query.count();
        List<HistoricProcessInstance> records = query.listPage(
                (int) page.offset(), (int) page.getSize());

        page.setTotal(total);
        page.setRecords(records);
        return page;
    }

    // ==================== Tasks ====================

    @Override
    public IPage<Task> getTodoTasks(Page<Task> page, String keyword) {
        LoginUser loginUser = getLoginUser();

        TaskQuery query = taskService.createTaskQuery()
                .or()
                .taskAssignee(loginUser.getUsername())
                .taskCandidateUser(loginUser.getUsername())
                .endOr()
                .orderByTaskCreateTime().desc();

        if (keyword != null && !keyword.isEmpty()) {
            query.taskNameLike("%" + keyword + "%");
        }

        long total = query.count();
        List<Task> records = query.listPage((int) page.offset(), (int) page.getSize());

        page.setTotal(total);
        page.setRecords(records);
        return page;
    }

    @Override
    public IPage<HistoricTaskInstance> getDoneTasks(Page<HistoricTaskInstance> page, String keyword) {
        LoginUser loginUser = getLoginUser();

        var query = historyService.createHistoricTaskInstanceQuery()
                .taskAssignee(loginUser.getUsername())
                .finished()
                .orderByHistoricTaskInstanceEndTime().desc();

        if (keyword != null && !keyword.isEmpty()) {
            query.taskNameLike("%" + keyword + "%");
        }

        long total = query.count();
        List<HistoricTaskInstance> records = query.listPage(
                (int) page.offset(), (int) page.getSize());

        page.setTotal(total);
        page.setRecords(records);
        return page;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void completeTask(String taskId, Map<String, Object> variables, String comment) {
        String userId = getLoginUser().getUsername();
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();

        if (task == null) {
            throw new BusinessException(ResultCode.TASK_NOT_FOUND);
        }

        // 添加审批意见
        if (comment != null && !comment.isEmpty()) {
            taskService.addComment(taskId, task.getProcessInstanceId(), comment);
        }

        // 设置流程变量
        if (variables != null) {
            taskService.setVariables(taskId, variables);
        }

        // 若未签收，先签收
        if (task.getAssignee() == null) {
            taskService.claim(taskId, userId);
        }

        taskService.complete(taskId);

        log.info("Completed task: {}, user: {}", taskId, userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rejectTask(String taskId, String comment) {
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (task == null) {
            throw new BusinessException(ResultCode.TASK_NOT_FOUND);
        }

        if (comment != null && !comment.isEmpty()) {
            taskService.addComment(taskId, task.getProcessInstanceId(), comment);
        }

        // 驳回：回退到上一个用户任务
        // 找到流程开始事件并回退
        Map<String, Object> variables = new HashMap<>();
        variables.put("rejected", true);
        variables.put("rejectedBy", getLoginUser().getUsername());

        // 简单驳回：重新分配给流程发起人
        var processInstance = runtimeService.createProcessInstanceQuery()
                .processInstanceId(task.getProcessInstanceId())
                .singleResult();

        if (comment != null && !comment.isEmpty()) {
            taskService.addComment(taskId, task.getProcessInstanceId(), "REJECT: " + comment);
        }

        // 驳回给发起人——创建用户任务供重新提交
        String starterUsername = (String) runtimeService.getVariable(
                task.getProcessInstanceId(), "starter");
        taskService.delegateTask(taskId, starterUsername);

        log.info("Rejected task: {}, back to: {}", taskId, starterUsername);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delegateTask(String taskId, String userId) {
        taskService.delegateTask(taskId, userId);
        log.info("Delegated task: {} to user: {}", taskId, userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void transferTask(String taskId, String userId) {
        taskService.setAssignee(taskId, userId);
        log.info("Transferred task: {} to user: {}", taskId, userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addSign(String taskId, List<String> userIds) {
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (task == null) {
            throw new BusinessException(ResultCode.TASK_NOT_FOUND);
        }

        for (String userId : userIds) {
            taskService.addCandidateUser(taskId, userId);
        }

        log.info("Add sign for task: {}, users: {}", taskId, userIds);
    }

    // ==================== BPMN Model ====================

    @Override
    public String getBpmnXml(String procDefId) {
        BpmnModel model = repositoryService.getBpmnModel(procDefId);
        if (model == null) {
            throw new BusinessException(ResultCode.DATA_NOT_EXIST);
        }
        BpmnXMLConverter converter = new BpmnXMLConverter();
        byte[] xmlBytes = converter.convertToXML(model);
        return new String(xmlBytes, StandardCharsets.UTF_8);
    }

    @Override
    public List<Map<String, String>> getUserTaskNodes(String procDefId) {
        BpmnModel model = repositoryService.getBpmnModel(procDefId);
        if (model == null) {
            return Collections.emptyList();
        }
        List<UserTask> userTasks = model.getMainProcess().findFlowElementsOfType(UserTask.class);
        List<Map<String, String>> result = new ArrayList<>();
        for (UserTask task : userTasks) {
            Map<String, String> node = new HashMap<>();
            node.put("nodeId", task.getId());
            node.put("nodeName", task.getName());
            node.put("assignee", task.getAssignee());
            result.add(node);
        }
        return result;
    }

    // ==================== History ====================

    @Override
    public List<HistoricActivityInstance> getProcessHistory(String procInstId) {
        return historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(procInstId)
                .orderByHistoricActivityInstanceStartTime().asc()
                .list();
    }

    @Override
    public List<Comment> getTaskComments(String taskId) {
        return taskService.getTaskComments(taskId);
    }

    // ==================== Helper ====================

    private LoginUser getLoginUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof LoginUser)) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        return (LoginUser) authentication.getPrincipal();
    }

    /**
     * Pagination helper for task queries with offset
     */
    private long calculateOffset(Page<?> page) {
        return page.offset();
    }
}
