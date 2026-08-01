package com.jinfu.flowable.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RejectRequest {

    @NotBlank(message = "驳回理由不能为空")
    private String comment;
}
