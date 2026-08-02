package com.jinfu.flowable.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jinfu.common.result.Result;
import com.jinfu.flowable.dto.*;
import com.jinfu.flowable.service.FlowProcessService;
import com.jinfu.security.annotation.RequiresPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.history.HistoricActivityInstance;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.task.Comment;
import org.flowable.task.api.Task;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/flow")
@RequiredArgsConstructor
@Tag(name = "Workflow Management", description = "Process definition, instance, task and history APIs")
public class FlowProcessController {

    private final FlowProcessService flowProcessService;

    // ==================== Process Definition ====================

    @GetMapping("/definition/list")
    @RequiresPermission("flow:definition:list")
    @Operation(summary = "Paginated process definition list")
    public Result<Map<String, Object>> listDefinitions(
            @Parameter(description = "Page number") @RequestParam(defaultValue = "1") int pageNum,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int pageSize,
            @Parameter(description = "Search keyword (name)") @RequestParam(required = false) String keyword) {

        Page<ProcessDefinition> page = new Page<>(pageNum, pageSize);
        IPage<ProcessDefinition> result = flowProcessService.getProcessDefinitions(page, keyword);

        List<Map<String, Object>> records = new ArrayList<>();
        for (ProcessDefinition pd : result.getRecords()) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", pd.getId());
            item.put("key", pd.getKey());
            item.put("name", pd.getName());
            item.put("version", pd.getVersion());
            item.put("deploymentId", pd.getDeploymentId());
            item.put("suspended", pd.isSuspended());
            records.add(item);
        }

        Map<String, Object> pageResult = new HashMap<>();
        pageResult.put("total", result.getTotal());
        pageResult.put("records", records);
        return Result.success(pageResult);
    }

    @PostMapping("/definition/deploy")
    @RequiresPermission("flow:definition:deploy")
    @Operation(summary = "Deploy process definition", description = "Deploy a BPMN 2.0 XML string")
    public Result<Void> deploy(@RequestBody Map<String, String> params) {
        String name = params.get("name");
        String bpmnXml = params.get("bpmnXml");
        flowProcessService.deployProcessDefinition(name, bpmnXml);
        return Result.success();
    }

    @DeleteMapping("/definition/{deploymentId}")
    @RequiresPermission("flow:definition:del")
    @Operation(summary = "Delete deployment")
    public Result<Void> deleteDeployment(@PathVariable String deploymentId) {
        flowProcessService.deleteDeployment(deploymentId);
        return Result.success();
    }

    @PutMapping("/definition/{procDefId}/suspend")
    @RequiresPermission("flow:definition:list")
    @Operation(summary = "Suspend process definition")
    public Result<Void> suspend(@PathVariable String procDefId) {
        flowProcessService.suspendDefinition(procDefId);
        return Result.success();
    }

    @PutMapping("/definition/{procDefId}/activate")
    @RequiresPermission("flow:definition:list")
    @Operation(summary = "Activate process definition")
    public Result<Void> activate(@PathVariable String procDefId) {
        flowProcessService.activateDefinition(procDefId);
        return Result.success();
    }

    // ==================== BPMN Model ====================

    @GetMapping("/definition/{procDefId}/xml")
    @RequiresPermission("flow:definition:list")
    @Operation(summary = "Get BPMN 2.0 XML for a process definition")
    public Result<String> getBpmnXml(@PathVariable String procDefId) {
        String xml = flowProcessService.getBpmnXml(procDefId);
        return Result.success(xml);
    }

    @GetMapping("/definition/{procDefId}/nodes")
    @RequiresPermission("flow:definition:list")
    @Operation(summary = "Get all user task nodes for a process definition")
    public Result<List<Map<String, String>>> getUserTaskNodes(@PathVariable String procDefId) {
        return Result.success(flowProcessService.getUserTaskNodes(procDefId));
    }

    // ==================== Process Instance ====================

    @PostMapping("/instance/start")
    @RequiresPermission("flow:instance:list")
    @Operation(summary = "Start process instance")
    public Result<Map<String, String>> startProcess(@Valid @RequestBody StartProcessRequest request) {
        String procInstId = flowProcessService.startProcess(
                request.getProcDefKey(), request.getBusinessKey(), request.getVariables());
        Map<String, String> result = new HashMap<>();
        result.put("processInstanceId", procInstId);
        return Result.success(result);
    }

    @GetMapping("/instance/list")
    @RequiresPermission("flow:instance:list")
    @Operation(summary = "My started process instances")
    public Result<Map<String, Object>> myInstances(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String keyword) {

        Page<HistoricProcessInstance> page = new Page<>(pageNum, pageSize);
        IPage<HistoricProcessInstance> result = flowProcessService.getAppliedProcesses(page, keyword);

        List<Map<String, Object>> records = new ArrayList<>();
        for (HistoricProcessInstance pi : result.getRecords()) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", pi.getId());
            item.put("processDefinitionId", pi.getProcessDefinitionId());
            item.put("processDefinitionName", pi.getProcessDefinitionName());
            item.put("processDefinitionKey", pi.getProcessDefinitionKey());
            item.put("businessKey", pi.getBusinessKey());
            item.put("startUserId", pi.getStartUserId());
            item.put("startTime", pi.getStartTime());
            item.put("endTime", pi.getEndTime());
            item.put("deleteReason", pi.getDeleteReason());
            records.add(item);
        }

        Map<String, Object> pageResult = new HashMap<>();
        pageResult.put("total", result.getTotal());
        pageResult.put("records", records);
        return Result.success(pageResult);
    }

    @DeleteMapping("/instance/{procInstId}")
    @RequiresPermission("flow:instance:list")
    @Operation(summary = "Cancel process instance")
    public Result<Void> cancelInstance(@PathVariable String procInstId,
                                       @RequestParam(defaultValue = "User cancelled") String reason) {
        flowProcessService.cancelProcess(procInstId, reason);
        return Result.success();
    }

    // ==================== Tasks ====================

    @GetMapping("/task/todo")
    @Operation(summary = "My to-do tasks")
    public Result<Map<String, Object>> todoTasks(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String keyword) {

        Page<Task> page = new Page<>(pageNum, pageSize);
        IPage<Task> result = flowProcessService.getTodoTasks(page, keyword);

        List<Map<String, Object>> records = new ArrayList<>();
        for (Task task : result.getRecords()) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", task.getId());
            item.put("name", task.getName());
            item.put("processInstanceId", task.getProcessInstanceId());
            item.put("processDefinitionId", task.getProcessDefinitionId());
            item.put("taskDefinitionKey", task.getTaskDefinitionKey());
            item.put("formKey", task.getFormKey());
            item.put("assignee", task.getAssignee());
            item.put("createTime", task.getCreateTime());
            records.add(item);
        }

        Map<String, Object> pageResult = new HashMap<>();
        pageResult.put("total", result.getTotal());
        pageResult.put("records", records);
        return Result.success(pageResult);
    }

    @GetMapping("/task/done")
    @Operation(summary = "My completed tasks")
    public Result<Map<String, Object>> doneTasks(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String keyword) {

        Page<HistoricTaskInstance> page = new Page<>(pageNum, pageSize);
        IPage<HistoricTaskInstance> result = flowProcessService.getDoneTasks(page, keyword);

        List<Map<String, Object>> records = new ArrayList<>();
        for (HistoricTaskInstance task : result.getRecords()) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", task.getId());
            item.put("name", task.getName());
            item.put("processInstanceId", task.getProcessInstanceId());
            item.put("processDefinitionId", task.getProcessDefinitionId());
            item.put("assignee", task.getAssignee());
            item.put("startTime", task.getCreateTime());
            item.put("endTime", task.getEndTime());
            item.put("durationInMillis", task.getDurationInMillis());
            records.add(item);
        }

        Map<String, Object> pageResult = new HashMap<>();
        pageResult.put("total", result.getTotal());
        pageResult.put("records", records);
        return Result.success(pageResult);
    }

    @GetMapping("/task/apply")
    @Operation(summary = "My applied processes")
    public Result<Map<String, Object>> applyTasks(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String keyword) {
        // 复用实例列表接口
        return myInstances(pageNum, pageSize, keyword);
    }

    @PostMapping("/task/{taskId}/complete")
    @Operation(summary = "Complete task (approve)", description = "Approve and complete the task")
    public Result<Void> completeTask(@PathVariable String taskId,
                                     @Valid @RequestBody CompleteTaskRequest request) {
        flowProcessService.completeTask(taskId, request.getVariables(), request.getComment());
        return Result.success();
    }

    @PostMapping("/task/{taskId}/reject")
    @Operation(summary = "Reject task", description = "Reject the task back to the starter")
    public Result<Void> rejectTask(@PathVariable String taskId,
                                   @Valid @RequestBody RejectRequest request) {
        flowProcessService.rejectTask(taskId, request.getComment());
        return Result.success();
    }

    @PostMapping("/task/{taskId}/delegate")
    @Operation(summary = "Delegate task", description = "Delegate the task to another user")
    public Result<Void> delegateTask(@PathVariable String taskId,
                                     @Valid @RequestBody DelegateRequest request) {
        flowProcessService.delegateTask(taskId, request.getUserId());
        return Result.success();
    }

    @PostMapping("/task/{taskId}/transfer")
    @Operation(summary = "Transfer task", description = "Transfer the task to another user")
    public Result<Void> transferTask(@PathVariable String taskId,
                                     @Valid @RequestBody DelegateRequest request) {
        flowProcessService.transferTask(taskId, request.getUserId());
        return Result.success();
    }

    @PostMapping("/task/{taskId}/addSign")
    @Operation(summary = "Add sign", description = "Add additional signers to the task")
    public Result<Void> addSign(@PathVariable String taskId,
                                @Valid @RequestBody AddSignRequest request) {
        flowProcessService.addSign(taskId, request.getUserIds());
        return Result.success();
    }

    // ==================== History ====================

    @GetMapping("/history/{procInstId}")
    @Operation(summary = "Process history", description = "Get all activity instances for a process")
    public Result<List<Map<String, Object>>> processHistory(@PathVariable String procInstId) {
        List<HistoricActivityInstance> activities = flowProcessService.getProcessHistory(procInstId);

        List<Map<String, Object>> records = new ArrayList<>();
        for (HistoricActivityInstance act : activities) {
            Map<String, Object> item = new HashMap<>();
            item.put("activityId", act.getActivityId());
            item.put("activityName", act.getActivityName());
            item.put("activityType", act.getActivityType());
            item.put("assignee", act.getAssignee());
            item.put("startTime", act.getStartTime());
            item.put("endTime", act.getEndTime());
            item.put("durationInMillis", act.getDurationInMillis());
            records.add(item);
        }
        return Result.success(records);
    }

    @GetMapping("/task/{taskId}/comments")
    @Operation(summary = "Get task comments")
    public Result<List<Map<String, Object>>> taskComments(@PathVariable String taskId) {
        List<Comment> comments = flowProcessService.getTaskComments(taskId);

        List<Map<String, Object>> records = new ArrayList<>();
        for (Comment comment : comments) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", comment.getId());
            item.put("userId", comment.getUserId());
            item.put("message", comment.getFullMessage());
            item.put("time", comment.getTime());
            item.put("type", comment.getType());
            records.add(item);
        }
        return Result.success(records);
    }
}
