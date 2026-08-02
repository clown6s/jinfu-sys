package com.jinfu.flowable.controller;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jinfu.common.exception.BusinessException;
import com.jinfu.common.result.Result;
import com.jinfu.common.result.ResultCode;
import com.jinfu.flowable.dto.ApprovalInstanceVO;
import com.jinfu.flowable.entity.ApprovalRequest;
import com.jinfu.flowable.entity.ProcessDesign;
import com.jinfu.flowable.mapper.ApprovalRequestMapper;
import com.jinfu.flowable.service.ApprovalBridgeService;
import com.jinfu.flowable.service.ProcessDesignService;
import com.jinfu.security.annotation.RequiresPermission;
import com.jinfu.security.entity.LoginUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.HistoryService;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.history.HistoricActivityInstance;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 审批桥接 Controller — 接管旧 /approval/** 前端 API，底层全部走 Flowable 引擎。
 *
 * 替代旧的：
 * - com.jinfu.approval.controller.ApprovalController (/approval/todo, /approval/done, /approval/complete)
 * - com.jinfu.approval.controller.ProcessInstanceController (/approval/instance/**)
 *
 * 模板管理 (/approval/template/**) 由 ProcessDesignController (/flow/design/**) 替代，
 * 前端模板页迁移到 Flowable 流程设计器。
 */
@Slf4j
@RestController
@RequestMapping("/approval")
@RequiredArgsConstructor
@Tag(name = "审批管理（Flowable桥接）", description = "兼容旧审批前端 API，底层走 Flowable 引擎")
public class ApprovalBridgeController {

    private final ApprovalBridgeService approvalBridgeService;
    private final ApprovalRequestMapper approvalRequestMapper;
    private final ProcessDesignService processDesignService;
    private final RuntimeService runtimeService;
    private final TaskService taskService;
    private final HistoryService historyService;
    private final RepositoryService repositoryService;

    // ==================== 待办 / 已办 ====================

    @GetMapping("/todo")
    @Operation(summary = "待我审批列表")
    public Result<Map<String, Object>> todoList(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @AuthenticationPrincipal LoginUser loginUser) {

        String userId = String.valueOf(loginUser.getUserId());
        long offset = (long) (pageNum - 1) * pageSize;

        List<Task> tasks = taskService.createTaskQuery()
                .taskAssignee(userId)
                .orderByTaskCreateTime().desc()
                .listPage((int) offset, pageSize);

        long total = taskService.createTaskQuery().taskAssignee(userId).count();

        List<ApprovalInstanceVO> records = tasks.stream()
                .map(task -> buildTaskVO(task, loginUser))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("records", records);
        result.put("total", total);
        return Result.success(result);
    }

    @GetMapping("/done")
    @Operation(summary = "我已审批列表")
    public Result<Map<String, Object>> doneList(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @AuthenticationPrincipal LoginUser loginUser) {

        String userId = String.valueOf(loginUser.getUserId());
        long offset = (long) (pageNum - 1) * pageSize;

        List<HistoricTaskInstance> tasks = historyService.createHistoricTaskInstanceQuery()
                .taskAssignee(userId)
                .finished()
                .orderByHistoricTaskInstanceEndTime().desc()
                .listPage((int) offset, pageSize);

        long total = historyService.createHistoricTaskInstanceQuery()
                .taskAssignee(userId)
                .finished()
                .count();

        List<ApprovalInstanceVO> records = tasks.stream()
                .map(this::buildDoneTaskVO)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("records", records);
        result.put("total", total);
        return Result.success(result);
    }

    // ==================== 审批操作 ====================

    @PostMapping("/complete")
    @Operation(summary = "审批操作（同意/驳回）")
    public Result<Void> completeApproval(
            @RequestBody Map<String, Object> request,
            @AuthenticationPrincipal LoginUser loginUser) {

        String taskId = (String) request.get("taskId");
        String action = (String) request.get("action");
        String comment = (String) request.get("comment");

        if (taskId == null || taskId.isEmpty()) {
            throw new BusinessException(ResultCode.PARAM_INVALID, "taskId 不能为空");
        }

        Long userId = loginUser.getUserId();
        String userName = loginUser.getNickname();

        if ("rejected".equals(action)) {
            approvalBridgeService.reject(taskId, userId, userName, comment);
        } else {
            approvalBridgeService.approve(taskId, userId, userName, comment);
        }

        return Result.success();
    }

    // ==================== 我的申请 ====================

    @GetMapping("/instance/my")
    @Operation(summary = "我的申请列表")
    public Result<Map<String, Object>> myApplications(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @AuthenticationPrincipal LoginUser loginUser) {

        Page<ApprovalRequest> page = new Page<>(pageNum, pageSize);
        IPage<ApprovalRequest> result = approvalRequestMapper.selectPage(page,
                new LambdaQueryWrapper<ApprovalRequest>()
                        .eq(ApprovalRequest::getInitiatorId, loginUser.getUserId())
                        .orderByDesc(ApprovalRequest::getId));

        List<ApprovalInstanceVO> records = result.getRecords().stream()
                .map(this::buildInstanceVO)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("records", records);
        resultMap.put("total", result.getTotal());
        return Result.success(resultMap);
    }

    // ==================== 审批详情 ====================

    @GetMapping("/instance/{id}")
    @Operation(summary = "审批详情（id = sys_approval_request.id）")
    public Result<ApprovalInstanceVO> getDetail(@PathVariable Long id,
                                                 @AuthenticationPrincipal LoginUser loginUser) {
        ApprovalRequest req = approvalBridgeService.getById(id);
        if (req == null) {
            throw new BusinessException(ResultCode.DATA_NOT_EXIST, "审批请求不存在");
        }

        // 越权校验：只有发起人、当前审批人、抄送人才能查看详情
        Long currentUserId = loginUser.getUserId();
        boolean isInitiator = currentUserId.equals(req.getInitiatorId());
        boolean isAssignee = !isInitiator && taskService.createTaskQuery()
                .processInstanceId(req.getProcessInstanceId())
                .active()
                .taskAssignee(String.valueOf(currentUserId))
                .count() > 0;
        boolean isCc = !isInitiator && !isAssignee && req.getCcUserIds() != null
                && req.getCcUserIds().contains(String.valueOf(currentUserId));
        if (!isInitiator && !isAssignee && !isCc) {
            throw new BusinessException(ResultCode.FORBIDDEN, "无权查看此审批详情");
        }

        return Result.success(buildInstanceVO(req));
    }

    // ==================== 撤销审批 ====================

    @PutMapping("/instance/{id}/cancel")
    @Operation(summary = "撤销审批（id = sys_approval_request.id）")
    public Result<Void> cancel(@PathVariable Long id,
                               @AuthenticationPrincipal LoginUser loginUser) {
        ApprovalRequest req = approvalBridgeService.getById(id);
        if (req == null) {
            throw new BusinessException(ResultCode.DATA_NOT_EXIST, "审批请求不存在");
        }
        approvalBridgeService.cancelApproval(req.getProcessInstanceId(),
                loginUser.getUserId(), "用户主动撤销");
        return Result.success();
    }

    // ==================== VO 组装方法 ====================

    /**
     * 从 Flowable Task + sys_approval_request 组装待办 VO
     */
    private ApprovalInstanceVO buildTaskVO(Task task, LoginUser loginUser) {
        ApprovalRequest req = approvalBridgeService.findByProcessInstanceId(task.getProcessInstanceId());
        if (req == null) return null;

        ApprovalInstanceVO vo = baseVO(req);
        vo.setStatus("pending");

        // 组装历史节点（传入当前 task，pending 节点会带 taskId）
        vo.setNodes(buildNodes(task.getProcessInstanceId(), task));

        // 当前步骤 = pending 节点的 stepOrder
        setCurrentStep(vo);
        return vo;
    }

    /**
     * 从 Flowable HistoricTaskInstance 组装已办 VO
     */
    private ApprovalInstanceVO buildDoneTaskVO(HistoricTaskInstance histTask) {
        ApprovalRequest req = approvalBridgeService.findByProcessInstanceId(histTask.getProcessInstanceId());
        if (req == null) return null;

        ApprovalInstanceVO vo = baseVO(req);
        vo.setStatus(determineStatus(req.getProcessInstanceId()));
        vo.setNodes(buildNodes(histTask.getProcessInstanceId(), null));
        setCurrentStep(vo);
        return vo;
    }

    /**
     * 从 sys_approval_request 组装完整 VO（我的申请 + 详情）
     */
    private ApprovalInstanceVO buildInstanceVO(ApprovalRequest req) {
        ApprovalInstanceVO vo = baseVO(req);
        vo.setStatus(determineStatus(req.getProcessInstanceId()));
        vo.setNodes(buildNodes(req.getProcessInstanceId(), null));
        setCurrentStep(vo);

        // 组装抄送人
        if (req.getCcUserIds() != null && !req.getCcUserIds().isEmpty()) {
            try {
                List<Long> ccIds = JSONUtil.toList(req.getCcUserIds(), Long.class);
                List<Map<String, Object>> ccUsers = new ArrayList<>();
                for (Long ccId : ccIds) {
                    Map<String, Object> u = new HashMap<>();
                    u.put("id", ccId);
                    ccUsers.add(u);
                }
                vo.setCcUsers(ccUsers);
            } catch (Exception ignored) {
            }
        }

        return vo;
    }

    /**
     * 提取公共字段到 VO
     */
    private ApprovalInstanceVO baseVO(ApprovalRequest req) {
        ApprovalInstanceVO vo = new ApprovalInstanceVO();
        vo.setId(req.getId());
        vo.setProcessInstanceId(req.getProcessInstanceId());
        vo.setProcessKey(req.getProcessKey());
        vo.setTitle(req.getTitle());
        vo.setFormId(req.getFormId());
        vo.setFormSchemaSnapshot(req.getFormSchemaSnapshot());
        vo.setFormData(req.getFormData());
        vo.setInitiatorId(req.getInitiatorId());
        vo.setInitiatorName(req.getInitiatorName());
        vo.setDeptId(req.getDeptId());

        // 从 ProcessDesign 获取流程名称
        ProcessDesign design = processDesignService.getOne(
                new LambdaQueryWrapper<ProcessDesign>()
                        .eq(ProcessDesign::getProcessKey, req.getProcessKey())
                        .orderByDesc(ProcessDesign::getVersion)
                        .last("LIMIT 1"));
        vo.setTemplateName(design != null ? design.getProcessName() : req.getProcessKey());

        // 从 BPMN 获取总步骤数
        ProcessDefinition procDef = repositoryService.createProcessDefinitionQuery()
                .processDefinitionKey(req.getProcessKey())
                .latestVersion()
                .singleResult();
        if (procDef != null) {
            var bpmnModel = repositoryService.getBpmnModel(procDef.getId());
            if (bpmnModel != null) {
                long userTaskCount = bpmnModel.getMainProcess().getFlowElements().stream()
                        .filter(e -> e instanceof org.flowable.bpmn.model.UserTask)
                        .count();
                vo.setTotalSteps((int) userTaskCount);
            }
        }

        // 创建时间从流程实例获取
        HistoricProcessInstance histProc = historyService.createHistoricProcessInstanceQuery()
                .processInstanceId(req.getProcessInstanceId())
                .singleResult();
        if (histProc != null) {
            vo.setCreateTime(toLocalDateTime(histProc.getStartTime()));
            if (histProc.getEndTime() != null) {
                vo.setUpdateTime(toLocalDateTime(histProc.getEndTime()));
            }
        }

        return vo;
    }

    /**
     * 从 Flowable 历史活动实例组装审批节点列表
     *
     * @param processInstanceId Flowable 流程实例ID
     * @param currentTask       当前 active task（待办场景传入），详情场景传 null 会自动查询
     */
    private List<ApprovalInstanceVO.ApprovalNodeInfo> buildNodes(String processInstanceId, Task currentTask) {
        // 详情页场景：自动查当前 active task
        if (currentTask == null) {
            currentTask = taskService.createTaskQuery()
                    .processInstanceId(processInstanceId)
                    .active()
                    .orderByTaskCreateTime().asc()
                    .listPage(0, 1)
                    .stream().findFirst().orElse(null);
        }

        List<HistoricActivityInstance> activities = historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(processInstanceId)
                .activityType("userTask")
                .orderByHistoricActivityInstanceStartTime().asc()
                .list();

        List<ApprovalInstanceVO.ApprovalNodeInfo> nodes = new ArrayList<>();
        int stepOrder = 1;
        for (HistoricActivityInstance act : activities) {
            ApprovalInstanceVO.ApprovalNodeInfo node = new ApprovalInstanceVO.ApprovalNodeInfo();
            node.setStepOrder(stepOrder++);
            node.setStepName(act.getActivityName());
            node.setCreateTime(toLocalDateTime(act.getStartTime()));
            node.setUpdateTime(toLocalDateTime(act.getEndTime()));

            // 解析审批人
            if (act.getAssignee() != null) {
                try {
                    node.setApproverId(Long.valueOf(act.getAssignee()));
                } catch (NumberFormatException ignored) {
                    node.setApproverValue(act.getAssignee());
                }
            }

            // 判断节点状态
            if (act.getEndTime() != null) {
                node.setAction("approved"); // 已完成的用户任务默认为 approved
            } else if (currentTask != null && act.getActivityId().equals(currentTask.getTaskDefinitionKey())) {
                node.setAction("pending");
                node.setTaskId(currentTask.getId()); // 给当前待办节点设置 taskId
            } else {
                node.setAction("pending");
            }

            nodes.add(node);
        }

        return nodes;
    }

    /**
     * 设置当前步骤序号 — 从 nodes 中找第一个 pending 节点的 stepOrder
     */
    private void setCurrentStep(ApprovalInstanceVO vo) {
        if (vo.getNodes() != null && !vo.getNodes().isEmpty()) {
            // 找第一个 pending 节点
            int current = vo.getNodes().stream()
                    .filter(n -> "pending".equals(n.getAction()))
                    .mapToInt(ApprovalInstanceVO.ApprovalNodeInfo::getStepOrder)
                    .findFirst()
                    .orElse(vo.getNodes().size()); // 全部完成则等于最后一步
            vo.setCurrentStep(current);
        }
    }

    /**
     * 判断流程实例当前状态
     */
    private String determineStatus(String processInstanceId) {
        ProcessInstance running = runtimeService.createProcessInstanceQuery()
                .processInstanceId(processInstanceId)
                .singleResult();
        if (running != null) {
            return "pending";
        }

        HistoricProcessInstance hist = historyService.createHistoricProcessInstanceQuery()
                .processInstanceId(processInstanceId)
                .singleResult();
        if (hist == null) {
            return "pending";
        }

        if (hist.getDeleteReason() != null) {
            String reason = hist.getDeleteReason().toLowerCase();
            if (reason.contains("reject")) {
                return "rejected";
            }
            return "cancelled";
        }

        return "approved";
    }

    /**
     * Date → LocalDateTime 转换
     */
    private java.time.LocalDateTime toLocalDateTime(Date date) {
        if (date == null) return null;
        return date.toInstant()
                .atZone(java.time.ZoneId.systemDefault())
                .toLocalDateTime();
    }
}
