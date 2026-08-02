package com.jinfu.flowable.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 审批实例详情 VO — 从 Flowable 流程实例 + sys_approval_request 组装，
 * 保持与旧自研审批链前端兼容的数据结构。
 */
@Data
public class ApprovalInstanceVO {

    /** sys_approval_request 主键（兼容前端用 id 做路由参数） */
    private Long id;

    /** Flowable 流程实例ID */
    private String processInstanceId;

    /** 流程定义Key */
    private String processKey;

    /** 流程名称（来自 ProcessDesign） */
    private String templateName;

    /** 表单定义ID */
    private Long formId;

    /** 表单 Schema 快照（JSON 字符串） */
    private String formSchemaSnapshot;

    /** 表单数据（JSON 字符串） */
    private String formData;

    /** 审批标题 */
    private String title;

    /** 发起人ID */
    private Long initiatorId;

    /** 发起人姓名 */
    private String initiatorName;

    /** 发起人部门ID */
    private Long deptId;

    /** 当前步骤序号（从1开始，基于 Flowable 历史活动节点计算） */
    private Integer currentStep;

    /** 总步骤数（BPMN 中用户任务节点总数） */
    private Integer totalSteps;

    /** 状态: pending | approved | rejected | cancelled */
    private String status;

    /** 创建时间（流程启动时间） */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;

    /** 审批节点记录列表（从 Flowable 历史活动+评论组装） */
    private List<ApprovalNodeInfo> nodes;

    /** 抄送人列表 */
    private List<Map<String, Object>> ccUsers;

    /**
     * 审批节点信息 — 从 Flowable HistoricActivityInstance + Comment 组装
     */
    @Data
    public static class ApprovalNodeInfo {
        private Integer stepOrder;
        private String stepName;
        private String approverType;
        private String approverValue;
        private Long approverId;
        private String approverName;
        /** pending | approved | rejected | cancelled */
        private String action;
        private String comment;
        private LocalDateTime createTime;
        private LocalDateTime updateTime;
        /** Flowable 任务ID（仅当前待办节点有值，前端用它调审批接口） */
        private String taskId;
    }
}
