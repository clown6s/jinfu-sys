package com.jinfu.approval.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 审批节点 VO
 */
@Data
public class ApprovalNodeVO {

    private Long id;
    private Long instanceId;
    private Integer stepOrder;
    private String stepName;
    private String approverType;
    private String approverValue;
    private Long approverId;
    private String approverName;
    private String action;
    private String comment;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
