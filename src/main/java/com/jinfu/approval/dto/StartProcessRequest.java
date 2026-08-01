package com.jinfu.approval.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 发起审批请求
 */
@Data
public class StartProcessRequest {

    /** 模板ID */
    @NotNull(message = "请选择审批模板")
    private Long templateId;

    /** 审批标题 */
    @NotBlank(message = "审批标题不能为空")
    private String title;

    /** 表单数据（JSON对象，key-value） */
    @NotNull(message = "请填写表单数据")
    private Map<String, Object> formData;

    /** 抄送人ID列表（可选） */
    private List<Long> ccUserIds;
}
