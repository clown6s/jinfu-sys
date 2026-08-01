package com.jinfu.approval.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.jinfu.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 审批流程模板 — 部门可自定义审批步骤链
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_process_template")
public class SysProcessTemplate extends BaseEntity {

    /** 模板名称 */
    private String templateName;

    /** 描述 */
    private String description;

    /** 所属部门ID（NULL=全公司可用） */
    private Long deptId;

    /** 关联表单定义ID */
    private Long formId;

    /**
     * 审批步骤链 JSON，格式：
     * [{"order":1,"name":"部门经理","approverType":"role","approverValue":"manager"}, ...]
     * approverType: specific_user | role | dept_leader
     */
    @TableField("step_chain")
    private String stepChain;

    /** 0=启用 1=停用 */
    private Integer status;
}
