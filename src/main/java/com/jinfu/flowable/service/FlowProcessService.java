package com.jinfu.flowable.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.flowable.engine.history.HistoricActivityInstance;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.task.Comment;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.task.api.Task;
import org.flowable.task.api.history.HistoricTaskInstance;

import java.util.List;
import java.util.Map;

public interface FlowProcessService {

    // ==================== Process Definition ====================
    IPage<ProcessDefinition> getProcessDefinitions(Page<ProcessDefinition> page, String keyword);

    void deployProcessDefinition(String name, String bpmnXml);

    void deleteDeployment(String deploymentId);

    void suspendDefinition(String procDefId);

    void activateDefinition(String procDefId);

    // ==================== Process Instance ====================
    String startProcess(String procDefKey, String businessKey, Map<String, Object> variables);

    void cancelProcess(String procInstId, String reason);

    IPage<HistoricProcessInstance> getAppliedProcesses(Page<HistoricProcessInstance> page, String keyword);

    // ==================== Tasks ====================
    IPage<Task> getTodoTasks(Page<Task> page, String keyword);

    IPage<HistoricTaskInstance> getDoneTasks(Page<HistoricTaskInstance> page, String keyword);

    void completeTask(String taskId, Map<String, Object> variables, String comment);

    void rejectTask(String taskId, String comment);

    void delegateTask(String taskId, String userId);

    void transferTask(String taskId, String userId);

    void addSign(String taskId, List<String> userIds);

    /** Get BPMN 2.0 XML for a process definition */
    String getBpmnXml(String procDefId);

    /** Get all user task nodes (id + name) from a process definition */
    List<Map<String, String>> getUserTaskNodes(String procDefId);

    // ==================== History ====================
    List<HistoricActivityInstance> getProcessHistory(String procInstId);

    List<Comment> getTaskComments(String taskId);
}
