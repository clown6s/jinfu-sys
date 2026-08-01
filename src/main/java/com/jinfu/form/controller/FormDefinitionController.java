package com.jinfu.form.controller;

import com.jinfu.common.result.Result;
import com.jinfu.form.entity.FormDefinition;
import com.jinfu.form.entity.FormFieldPermission;
import com.jinfu.form.service.FormDefinitionService;
import com.jinfu.form.service.FormFieldPermissionService;
import com.jinfu.security.annotation.RequiresPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/form/definition")
@RequiredArgsConstructor
@Tag(name = "Form Definition", description = "Dynamic form definition CRUD and designer APIs")
public class FormDefinitionController {

    private final FormDefinitionService formDefinitionService;
    private final FormFieldPermissionService formFieldPermissionService;

    @GetMapping("/list")
    @RequiresPermission("form:definition:list")
    @Operation(summary = "Form definition list")
    public Result<List<FormDefinition>> list(
            @Parameter(description = "Search keyword") @RequestParam(required = false) String keyword,
            @Parameter(description = "Status: 0=Draft 1=Published 2=Deprecated") @RequestParam(required = false) Integer status) {
        return Result.success(formDefinitionService.list(keyword, status));
    }

    @GetMapping("/{id}")
    @RequiresPermission("form:definition:list")
    @Operation(summary = "Get form definition by ID")
    public Result<Map<String, Object>> getById(@PathVariable Long id) {
        FormDefinition formDef = formDefinitionService.getById(id);
        List<FormFieldPermission> perms = formFieldPermissionService.getByFormId(id);

        Map<String, Object> result = new HashMap<>();
        result.put("formDefinition", formDef);
        result.put("fieldPermissions", perms);
        return Result.success(result);
    }

    @GetMapping("/key/{formKey}")
    @RequiresPermission("form:definition:list")
    @Operation(summary = "Get form definition by form key")
    public Result<FormDefinition> getByFormKey(@PathVariable String formKey) {
        return Result.success(formDefinitionService.getByFormKey(formKey));
    }

    @PostMapping
    @RequiresPermission("form:definition:add")
    @Operation(summary = "Create form definition")
    public Result<Void> add(@Valid @RequestBody FormDefinition formDef) {
        formDefinitionService.insert(formDef);
        return Result.success();
    }

    @PutMapping
    @RequiresPermission("form:definition:edit")
    @Operation(summary = "Update form definition")
    public Result<Void> update(@Valid @RequestBody FormDefinition formDef) {
        formDefinitionService.update(formDef);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @RequiresPermission("form:definition:del")
    @Operation(summary = "Delete form definition")
    public Result<Void> delete(@PathVariable Long id) {
        formDefinitionService.delete(id);
        formFieldPermissionService.deleteByFormId(id);
        return Result.success();
    }

    @PutMapping("/{id}/publish")
    @RequiresPermission("form:definition:edit")
    @Operation(summary = "Publish form definition")
    public Result<Void> publish(@PathVariable Long id) {
        formDefinitionService.publish(id);
        return Result.success();
    }

    @PutMapping("/{id}/deprecate")
    @RequiresPermission("form:definition:edit")
    @Operation(summary = "Deprecate form definition")
    public Result<Void> deprecate(@PathVariable Long id) {
        formDefinitionService.deprecate(id);
        return Result.success();
    }
}
