package com.jinfu.flowable.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 流程设计保存请求（id 为空=新建，非空=更新）
 */
@Data
public class ProcessDesignSaveRequest {

    private Long id;

    @NotBlank(message = "流程名称不能为空")
    @Size(max = 100, message = "流程名称不能超过100字符")
    private String processName;

    @NotBlank(message = "流程Key不能为空")
    @Size(max = 100, message = "流程Key不能超过100字符")
    @Pattern(regexp = "^[a-zA-Z_][a-zA-Z0-9_-]*$", message = "流程Key须以字母/下划线开头，仅含字母数字下划线连字符")
    private String processKey;

    @NotBlank(message = "BPMN XML 不能为空")
    private String bpmnXml;

    @Size(max = 500, message = "备注不能超过500字符")
    private String remark;
}
