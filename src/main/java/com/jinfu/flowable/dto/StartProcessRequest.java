package com.jinfu.flowable.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.Map;

@Data
public class StartProcessRequest {

    @NotBlank(message = "流程定义Key不能为空")
    private String procDefKey;

    @NotBlank(message = "业务Key不能为空")
    private String businessKey;

    private Long formInstanceId;
    private Map<String, Object> variables;
}
