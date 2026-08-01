package com.jinfu.form.controller;

import com.jinfu.common.result.Result;
import com.jinfu.form.entity.FormInstance;
import com.jinfu.form.service.FormInstanceService;
import com.jinfu.security.entity.LoginUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/form/instance")
@RequiredArgsConstructor
@Tag(name = "Form Instance", description = "Form instance business data save/load")
public class FormInstanceController {

    private final FormInstanceService formInstanceService;

    @GetMapping("/{id}")
    @Operation(summary = "Get form instance by ID")
    public Result<FormInstance> getById(@PathVariable Long id) {
        return Result.success(formInstanceService.getById(id));
    }

    @GetMapping("/procInst/{procInstId}")
    @Operation(summary = "Get form instance by process instance ID")
    public Result<FormInstance> getByProcInstId(@PathVariable String procInstId) {
        return Result.success(formInstanceService.getByProcInstId(procInstId));
    }

    @PostMapping
    @Operation(summary = "Save form instance (create or update)")
    public Result<Map<String, Long>> save(@RequestBody Map<String, Object> body) {
        Long formInstanceId = body.get("id") != null ? Long.valueOf(body.get("id").toString()) : null;
        String formKey = (String) body.get("formKey");
        String procInstId = (String) body.get("procInstId");
        String title = (String) body.get("title");
        String businessDataJson = body.get("businessDataJson") != null
                ? body.get("businessDataJson").toString() : null;

        Long creator = getLoginUserId();

        Long id = formInstanceService.save(formInstanceId, formKey, procInstId,
                title, creator, businessDataJson);

        return Result.success(Map.of("id", id));
    }

    @PutMapping("/{id}/data")
    @Operation(summary = "Update form instance business data")
    public Result<Void> updateData(@PathVariable Long id,
                                   @RequestBody Map<String, String> body) {
        formInstanceService.updateBusinessData(id, body.get("businessDataJson"));
        return Result.success();
    }

    @PutMapping("/{id}/bindProcInst")
    @Operation(summary = "Bind form instance to process instance")
    public Result<Void> bindProcInst(@PathVariable Long id,
                                     @RequestBody Map<String, String> body) {
        formInstanceService.bindProcInst(id, body.get("procInstId"));
        return Result.success();
    }

    private Long getLoginUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof LoginUser loginUser) {
            return loginUser.getUserId();
        }
        return null;
    }
}
