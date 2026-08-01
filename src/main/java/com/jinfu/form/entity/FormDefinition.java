package com.jinfu.form.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.jinfu.common.entity.BaseEntity;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("form_definition")
public class FormDefinition extends BaseEntity {

    /** Unique business key (e.g. leave_form) */
    @NotBlank(message = "表单编码不能为空")
    private String formKey;

    /** Form Name */
    @NotBlank(message = "表单名称不能为空")
    private String name;

    /** Description */
    private String description;

    /** JSON Schema (field definitions) */
    private String schemaJson;

    /** Version Number */
    private Integer version;

    /** 0=Draft 1=Published 2=Deprecated */
    private Integer status;
}
