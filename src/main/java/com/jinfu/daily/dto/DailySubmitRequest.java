package com.jinfu.daily.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.Map;

/**
 * 提交日报请求
 */
@Data
public class DailySubmitRequest {

    /** 填报日期（默认当天） */
    private LocalDate reportDate;

    /** 表单数据（JSON对象，key-value） */
    @NotNull(message = "请填写日报内容")
    private Map<String, Object> formData;
}
