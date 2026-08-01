package com.jinfu.approval.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 审批节点记录 — 每个审批步骤的操作记录
 */
@Data
@TableName("sys_approval_node")
public class SysApprovalNode {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 审批实例ID */
    private Long instanceId;

    /** 步骤序号 */
    private Integer stepOrder;

    /** 步骤名称 */
    private String stepName;

    /** 审批人类型: specific_user | role | dept_leader */
    private String approverType;

    /** 审批人值: userId / roleKey / deptId */
    private String approverValue;

    /** 实际审批人ID */
    private Long approverId;

    /** 实际审批人姓名 */
    private String approverName;

    /** pending=待审批 approved=同意 rejected=驳回 transferred=转交 */
    private String action;

    /** 审批意见 */
    private String comment;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
