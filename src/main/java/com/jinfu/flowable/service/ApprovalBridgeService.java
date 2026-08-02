package com.jinfu.flowable.service;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.jinfu.common.exception.BusinessException;
import com.jinfu.common.result.ResultCode;
import com.jinfu.flowable.entity.ApprovalRequest;
import com.jinfu.flowable.event.ApprovalFinishedEvent;
import com.jinfu.flowable.mapper.ApprovalRequestMapper;
import com.jinfu.form.entity.FormDefinition;
import com.jinfu.form.mapper.FormDefinitionMapper;
import com.jinfu.message.service.MessageService;
import com.jinfu.system.entity.SysUser;
import com.jinfu.system.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * 审批桥接层 — 在 Flowable 引擎之上补齐自研审批链的业务能力：
 *
 * 1. 表单 Schema 快照（Flowable 不管理表单）
 * 2. WS 推送表单数据给审批人（Flowable 无 WS）
 * 3. 抄送通知（Flowable 无内置抄送）
 * 4. ApprovalFinishedEvent 事件发布（Flowable 无业务事件）
 * 5. 审批人动态解析（通过 ApproverResolver Bean + BPMN 表达式）
 *
 * 日报等业务模块调用此服务，不再直接依赖 Flowable 引擎 API。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ApprovalBridgeService {

    private final ApprovalRequestMapper approvalRequestMapper;
    private final FormDefinitionMapper formDefinitionMapper;
    private final SysUserMapper sysUserMapper;
    private final RuntimeService runtimeService;
    private final TaskService taskService;
    private final MessageService messageService;
    private final ApplicationEventPublisher eventPublisher;

    // ==================== 发起审批 ====================

    /**
     * 发起审批
     *
     * @param processKey   Flowable 流程定义Key（BPMN process id）
     * @param businessKey  业务Key（如 "daily_report:{reportId}"）
     * @param title        审批标题
     * @param formId       表单定义ID（用于做 Schema 快照）
     * @param formData     表单数据
     * @param initiatorId  发起人ID
     * @param initiatorName 发起人姓名
     * @param deptId       发起人部门ID
     * @param ccUserIds    抄送人ID列表（可为空）
     * @return ApprovalRequest（含 processInstanceId）
     */
    @Transactional(rollbackFor = Exception.class)
    public ApprovalRequest startApproval(
            String processKey, String businessKey, String title,
            Long formId, Map<String, Object> formData,
            Long initiatorId, String initiatorName, Long deptId,
            List<Long> ccUserIds) {

        // 1. 加载表单定义做 Schema 快照
        FormDefinition formDef = formDefinitionMapper.selectById(formId);
        if (formDef == null) {
            throw new BusinessException(ResultCode.FORM_DEF_NOT_FOUND, "表单定义不存在");
        }

        // 2. 组装流程变量
        Map<String, Object> variables = new HashMap<>();
        variables.put("initiatorId", initiatorId);
        variables.put("initiatorName", initiatorName);
        variables.put("deptId", deptId);
        variables.put("title", title);
        if (formData != null) {
            variables.putAll(formData);
        }

        // 3. 启动 Flowable 流程实例
        ProcessInstance procInstance = runtimeService.startProcessInstanceByKey(
                processKey, businessKey, variables);

        // 4. 保存审批业务数据
        ApprovalRequest request = new ApprovalRequest();
        request.setProcessInstanceId(procInstance.getId());
        request.setProcessKey(processKey);
        request.setBusinessKey(businessKey);
        request.setTitle(title);
        request.setFormId(formId);
        request.setFormSchemaSnapshot(formDef.getSchemaJson());
        request.setFormData(JSONUtil.toJsonStr(formData));
        request.setInitiatorId(initiatorId);
        request.setInitiatorName(initiatorName);
        request.setDeptId(deptId);
        if (ccUserIds != null && !ccUserIds.isEmpty()) {
            request.setCcUserIds(JSONUtil.toJsonStr(ccUserIds));
        }
        approvalRequestMapper.insert(request);

        // 5. 推送待办通知给第一个审批人
        pushTaskNotification(procInstance.getId(), request);

        // 6. 推送抄送通知
        if (ccUserIds != null && !ccUserIds.isEmpty()) {
            for (Long ccId : ccUserIds) {
                messageService.sendToUser(ccId, MessageService.TYPE_CC, "审批抄送",
                        String.format("%s 发起了审批【%s】，请知悉", initiatorName, title),
                        request.getId());
            }
        }

        log.info("审批已发起: requestId={}, procInstId={}, title={}",
                request.getId(), procInstance.getId(), title);
        return request;
    }

    // ==================== 审批操作 ====================

    /**
     * 同意审批
     *
     * @param taskId    Flowable 任务ID
     * @param userId    操作人ID
     * @param userName  操作人姓名
     * @param comment   审批意见
     */
    @Transactional(rollbackFor = Exception.class)
    public void approve(String taskId, Long userId, String userName, String comment) {
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (task == null) {
            throw new BusinessException(ResultCode.TASK_NOT_FOUND);
        }

        // 越权校验：只有当前任务的审批人才能操作
        if (task.getAssignee() == null || !String.valueOf(userId).equals(task.getAssignee())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "无权操作此审批任务");
        }

        // 添加审批意见
        if (comment != null && !comment.isEmpty()) {
            taskService.addComment(taskId, task.getProcessInstanceId(),
                    String.format("[同意] %s: %s", userName, comment));
        }

        // 认领并完成
        if (task.getAssignee() == null) {
            taskService.claim(taskId, String.valueOf(userId));
        }
        taskService.complete(taskId);

        // 检查流程是否结束，若结束则发事件
        checkProcessFinished(task.getProcessInstanceId());

        // 若未结束，推送通知给下一个审批人
        pushTaskNotification(task.getProcessInstanceId(), null);

        log.info("审批同意: taskId={}, user={}", taskId, userName);
    }

    /**
     * 驳回审批 — 驳回到发起人
     *
     * @param taskId    Flowable 任务ID
     * @param userId    操作人ID
     * @param userName  操作人姓名
     * @param comment   驳回理由
     */
    @Transactional(rollbackFor = Exception.class)
    public void reject(String taskId, Long userId, String userName, String comment) {
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (task == null) {
            throw new BusinessException(ResultCode.TASK_NOT_FOUND);
        }

        // 越权校验：只有当前任务的审批人才能操作
        if (task.getAssignee() == null || !String.valueOf(userId).equals(task.getAssignee())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "无权操作此审批任务");
        }

        String procInstId = task.getProcessInstanceId();

        if (comment != null && !comment.isEmpty()) {
            taskService.addComment(taskId, procInstId,
                    String.format("[驳回] %s: %s", userName, comment));
        }

        // 终止流程实例（标记为 rejected）
        runtimeService.deleteProcessInstance(procInstId,
                "Rejected by " + userName + ": " + comment);

        // 发布驳回事件
        ApprovalRequest req = findByProcessInstanceId(procInstId);
        if (req != null) {
            messageService.sendToUser(req.getInitiatorId(), MessageService.TYPE_APPROVAL,
                    "审批被驳回",
                    String.format("您的审批【%s】已被驳回：%s", req.getTitle(), comment),
                    req.getId());

            eventPublisher.publishEvent(new ApprovalFinishedEvent(
                    req.getId(), procInstId, "rejected", req.getTitle(), req.getInitiatorId()));
        }

        log.info("审批驳回: taskId={}, procInstId={}, user={}", taskId, procInstId, userName);
    }

    /**
     * 转办任务
     */
    @Transactional(rollbackFor = Exception.class)
    public void transferTask(String taskId, Long targetUserId, String userName) {
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (task == null) {
            throw new BusinessException(ResultCode.TASK_NOT_FOUND);
        }
        taskService.setAssignee(taskId, String.valueOf(targetUserId));
        log.info("任务转办: taskId={}, from={}, to={}", taskId, userName, targetUserId);

        // 通知新审批人
        pushTaskNotification(task.getProcessInstanceId(), null);
    }

    /**
     * 撤销审批（发起人撤销）
     */
    @Transactional(rollbackFor = Exception.class)
    public void cancelApproval(String processInstanceId, Long userId, String reason) {
        ApprovalRequest req = findByProcessInstanceId(processInstanceId);
        if (req == null) {
            throw new BusinessException(ResultCode.DATA_NOT_EXIST, "审批请求不存在");
        }
        if (!userId.equals(req.getInitiatorId())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "只能撤销自己发起的审批");
        }

        runtimeService.deleteProcessInstance(processInstanceId,
                "Cancelled by initiator: " + reason);

        eventPublisher.publishEvent(new ApprovalFinishedEvent(
                req.getId(), processInstanceId, "cancelled", req.getTitle(), req.getInitiatorId()));

        log.info("审批撤销: procInstId={}, user={}", processInstanceId, userId);
    }

    // ==================== 查询 ====================

    /**
     * 通过 Flowable 流程实例ID 查审批业务数据
     */
    public ApprovalRequest findByProcessInstanceId(String processInstanceId) {
        return approvalRequestMapper.selectOne(new LambdaQueryWrapper<ApprovalRequest>()
                .eq(ApprovalRequest::getProcessInstanceId, processInstanceId)
                .last("LIMIT 1"));
    }

    /**
     * 通过 ID 查审批业务数据
     */
    public ApprovalRequest getById(Long id) {
        return approvalRequestMapper.selectById(id);
    }

    /**
     * 通过 businessKey 查审批业务数据
     */
    public ApprovalRequest findByBusinessKey(String businessKey) {
        return approvalRequestMapper.selectOne(new LambdaQueryWrapper<ApprovalRequest>()
                .eq(ApprovalRequest::getBusinessKey, businessKey)
                .orderByDesc(ApprovalRequest::getId)
                .last("LIMIT 1"));
    }

    // ==================== 私有方法 ====================

    /**
     * 推送待办通知给当前任务的审批人
     */
    private void pushTaskNotification(String processInstanceId, ApprovalRequest preloadedReq) {
        Task currentTask = taskService.createTaskQuery()
                .processInstanceId(processInstanceId)
                .active()
                .orderByTaskCreateTime().asc()
                .listPage(0, 1)
                .stream().findFirst().orElse(null);

        if (currentTask == null) {
            return; // 流程已结束
        }

        ApprovalRequest req = preloadedReq;
        if (req == null) {
            req = findByProcessInstanceId(processInstanceId);
        }
        if (req == null) {
            return;
        }

        // 解析审批人ID
        String assignee = currentTask.getAssignee();
        if (assignee == null || assignee.isEmpty()) {
            // 候选人场景：查候选用户
            return;
        }

        Long approverId;
        try {
            approverId = Long.valueOf(assignee);
        } catch (NumberFormatException e) {
            log.warn("审批人 assignee 不是数字ID: {}", assignee);
            return;
        }

        // 组装扩展数据：表单数据 + Schema + 节点信息
        Map<String, Object> extra = new HashMap<>();
        extra.put("bizType", "approval");
        extra.put("requestId", req.getId());
        extra.put("processInstanceId", processInstanceId);
        extra.put("taskId", currentTask.getId());
        extra.put("taskName", currentTask.getName());
        extra.put("formId", req.getFormId());
        extra.put("formData", req.getFormData());
        extra.put("formSchema", req.getFormSchemaSnapshot());
        extra.put("initiatorId", req.getInitiatorId());
        extra.put("initiatorName", req.getInitiatorName());
        extra.put("title", req.getTitle());

        String msg = String.format("您有新的审批待办：【%s】—— %s",
                req.getTitle(), currentTask.getName());
        messageService.sendToUser(approverId, MessageService.TYPE_APPROVAL,
                "新的审批待办", msg, req.getId(), extra);
    }

    /**
     * 检查流程是否已结束，若结束则发布终态事件
     */
    private void checkProcessFinished(String processInstanceId) {
        ProcessInstance procInstance = runtimeService.createProcessInstanceQuery()
                .processInstanceId(processInstanceId)
                .singleResult();

        if (procInstance == null) {
            // 流程已结束（运行时表无记录）
            ApprovalRequest req = findByProcessInstanceId(processInstanceId);
            if (req != null) {
                messageService.sendToUser(req.getInitiatorId(), MessageService.TYPE_APPROVAL,
                        "审批已通过",
                        String.format("您的审批【%s】已全部通过", req.getTitle()),
                        req.getId());

                eventPublisher.publishEvent(new ApprovalFinishedEvent(
                        req.getId(), processInstanceId, "approved",
                        req.getTitle(), req.getInitiatorId()));
            }
        }
    }
}
