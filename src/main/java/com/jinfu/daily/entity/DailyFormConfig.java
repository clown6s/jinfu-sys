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

    /** 日志类型ID（日报/周报/月报等） */
    @NotNull(message = "请选择日志类型")
    private Long logTypeId;

    /** 部门ID（联合唯一：deptId + logTypeId） */
    @NotNull(message = "请选择部门")
    private Long deptId;

    /** 关联表单定义ID（各部门各类型日志表单可不同） */
    @NotNull(message = "请选择日志表单")
    private Long formId;

    /** 关联审批模板ID（NULL=日报不需审批；过渡期兼容旧自研审批模板表） */
    private Long processTemplateId;

    /** Flowable 流程定义Key（BPMN process id）；配置后日报走 Flowable 审批 */
    private String processKey;

    /** 填报截止时间 HH:mm */
    private String reportTime;

    /** 0=停用 1=启用 */
    private Integer enabled;
}
