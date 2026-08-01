package com.jinfu.daily.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.jinfu.common.entity.BaseEntity;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 部门日报表单配置 — 每个部门绑定各自的日报表单，可绑定审批模板
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("daily_form_config")
public class DailyFormConfig extends BaseEntity {

    /** 部门ID（唯一绑定） */
    @NotNull(message = "请选择部门")
    private Long deptId;

    /** 关联表单定义ID（各部门日报表单可不同） */
    @NotNull(message = "请选择日报表单")
    private Long formId;

    /** 关联审批模板ID（NULL=日报不需审批） */
    private Long processTemplateId;

    /** 填报截止时间 HH:mm */
    private String reportTime;

    /** 0=停用 1=启用 */
    private Integer enabled;
}
