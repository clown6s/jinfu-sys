package com.jinfu.daily.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.jinfu.common.entity.BaseEntity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 日志类型 — 定义日报、周报、月报、项目日志等多种日志类型
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("log_type")
public class LogType extends BaseEntity {

    /** 类型编码（唯一标识，如 daily/weekly/monthly/project） */
    @NotBlank(message = "类型编码不能为空")
    private String code;

    /** 类型名称（如：日报、周报、月报） */
    @NotBlank(message = "类型名称不能为空")
    private String name;

    /** 排序号（越小越靠前） */
    @NotNull(message = "排序号不能为空")
    private Integer sortOrder;

    /** 0=停用 1=启用 */
    private Integer enabled;

    /** 描述说明 */
    private String description;
}
