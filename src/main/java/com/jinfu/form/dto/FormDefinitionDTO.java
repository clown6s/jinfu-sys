package com.jinfu.form.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
public class FormDefinitionDTO {

    private Long id;

    @NotBlank(message = "表单编码不能为空")
    private String formKey;

    @NotBlank(message = "表单名称不能为空")
    private String name;

    private String description;

    @NotBlank(message = "表单Schema不能为空")
    private String schemaJson;

    private Integer version;
    private Integer status;
    private List<FieldPermissionGroup> fieldPermissions;
}

@Data
class FieldPermissionGroup {
    /** BPMN Node ID */
    private String nodeId;
    /** Field permission list for this node */
    private List<FieldPermissionItem> permissions;
}

@Data
class FieldPermissionItem {
    /** Field key */
    private String fieldKey;
    /** edit / readonly / required / hidden */
    private String permission;
}
