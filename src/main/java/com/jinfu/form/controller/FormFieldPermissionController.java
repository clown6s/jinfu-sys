package com.jinfu.form.controller;

import com.jinfu.common.result.Result;
import com.jinfu.form.entity.FormFieldPermission;
import com.jinfu.form.entity.FormFieldPermission.FieldPermItem;
import com.jinfu.form.service.FormFieldPermissionService;
import com.jinfu.security.annotation.RequiresPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/form/permission")
@RequiredArgsConstructor
@Tag(name = "Form Field Permission", description = "Field-level permission per BPMN node")
public class FormFieldPermissionController {

    private final FormFieldPermissionService formFieldPermissionService;

    @PostMapping("/{formId}/node/{nodeId}")
    @RequiresPermission("form:designer:edit")
    @Operation(summary = "Save field permissions for a node",
               description = "Batch save field permissions for a specific BPMN node")
    public Result<Void> savePermissions(
            @PathVariable Long formId,
            @PathVariable String nodeId,
            @RequestBody Map<String, Object> body) {

        String procDefId = (String) body.get("procDefId");
        @SuppressWarnings("unchecked")
        List<Map<String, String>> permList = (List<Map<String, String>>) body.get("permissions");

        List<FieldPermItem> items = permList.stream()
                .map(m -> new FieldPermItem(m.get("fieldKey"), m.get("permission")))
                .toList();

        formFieldPermissionService.savePermissions(formId, procDefId, nodeId, items);
        return Result.success();
    }

    @GetMapping("/{formId}/node/{nodeId}")
    @Operation(summary = "Get field permissions for a node")
    public Result<List<FormFieldPermission>> getByNode(
            @PathVariable Long formId,
            @PathVariable String nodeId) {
        return Result.success(formFieldPermissionService.getByFormAndNode(formId, nodeId));
    }

    @GetMapping("/{formId}")
    @Operation(summary = "Get all field permissions for a form")
    public Result<List<FormFieldPermission>> getByForm(@PathVariable Long formId) {
        return Result.success(formFieldPermissionService.getByFormId(formId));
    }

    @DeleteMapping("/{formId}")
    @RequiresPermission("form:designer:edit")
    @Operation(summary = "Delete all field permissions for a form")
    public Result<Void> deleteByForm(@PathVariable Long formId) {
        formFieldPermissionService.deleteByFormId(formId);
        return Result.success();
    }
}
