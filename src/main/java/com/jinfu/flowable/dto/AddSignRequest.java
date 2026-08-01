package com.jinfu.flowable.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class AddSignRequest {

    @NotEmpty(message = "加签用户列表不能为空")
    private List<String> userIds;
}
