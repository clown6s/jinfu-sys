package com.jinfu.approval.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.jinfu.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 审批流程实例 — 用户发起的具体审批
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_process_instance")
public class SysProcessInstance extends BaseEntity {

    /** 模板ID */
    private Long templateId;

    /** 模板名称快照 */
    private String templateName;

    /** 表单定义ID */
    private Long formId;

    /** 表单Schema快照（JSON，防止后续表单修改影响已发起审批） */
    @TableField("form_schema_snapshot")
    private String formSchemaSnapshot;

    /** 用户填写的表单数据 JSON */
    @TableField("form_data")
    private String formData;

    /** 审批标题 */
    private String title;

    /** 发起人ID */
    private Long initiatorId;

    /** 发起人姓名 */
    private String initiatorName;

    /** 发起部门ID */
    private Long deptId;

    /** 当前步骤序号（从1开始） */
    private Integer currentStep;

    /** 总步骤数 */
    private Integer totalSteps;

    /** 步骤链快照 JSON */
    @TableField("step_chain_snapshot")
    private String stepChainSnapshot;

    /** pending=审批中 approved=已通过 rejected=已驳回 cancelled=已撤销 */
    private String status;

    /** 抄送人信息 JSON（[{id, name}, ...]） */
    @TableField("cc_users")
    private String ccUsers;
}
