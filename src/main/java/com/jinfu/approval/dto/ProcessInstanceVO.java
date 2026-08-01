package com.jinfu.approval.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 审批实例详情 VO（包含审批节点记录）
 */
@Data
public class ProcessInstanceVO {

    private Long id;
    private Long templateId;
    private String templateName;
    private Long formId;
    private String formSchemaSnapshot;
    private String formData;
    private String title;
    private Long initiatorId;
    private String initiatorName;
    private Long deptId;
    private Integer currentStep;
    private Integer totalSteps;
    private String status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    /** 审批节点记录列表 */
    private List<ApprovalNodeVO> nodes;

    /** 抄送人列表 */
    private List<Map<String, Object>> ccUsers;
}
