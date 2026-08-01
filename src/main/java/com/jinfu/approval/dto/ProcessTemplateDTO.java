package com.jinfu.approval.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 审批模板创建/更新请求
 */
@Data
public class ProcessTemplateDTO {

    /** 模板名称 */
    @NotBlank(message = "模板名称不能为空")
    private String templateName;

    /** 描述 */
    private String description;

    /** 所属部门ID */
    private Long deptId;

    /** 关联表单ID */
    @NotNull(message = "请选择关联表单")
    private Long formId;

    /** 审批步骤链 */
    @NotNull(message = "请配置审批步骤")
    private List<StepNode> stepChain;

    /** 0=启用 1=停用 */
    private Integer status;

    @Data
    public static class StepNode {
        /** 步骤序号（从1开始） */
        @NotNull
        private Integer order;
        /** 步骤名称 */
        @NotBlank
        private String name;
        /** 审批人类型: specific_user | role | dept_leader */
        @NotBlank
        private String approverType;
        /** 审批人值 */
        @NotBlank
        private String approverValue;
        /** 条件表达式（可选，如 "amount > 5000" 或 "leave_days > 3"） */
        private String condition;
    }
}
