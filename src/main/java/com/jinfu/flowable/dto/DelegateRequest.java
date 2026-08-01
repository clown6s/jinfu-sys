package com.jinfu.flowable.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class DelegateRequest {

    @NotBlank(message = "用户ID不能为空")
    private String userId;
}
