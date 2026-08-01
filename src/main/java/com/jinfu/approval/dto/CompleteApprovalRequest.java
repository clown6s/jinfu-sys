package com.jinfu.approval.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 审批操作请求（同意/拒绝）
 */
@Data
public class CompleteApprovalRequest {

    /** 审批节点ID */
    @NotNull(message = "审批节点ID不能为空")
    private Long nodeId;

    /** 操作: approved=同意 rejected=驳回 */
    @NotBlank(message = "操作类型不能为空")
    private String action;

    /** 审批意见 */
    private String comment;
}
