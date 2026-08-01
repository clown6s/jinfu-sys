package com.jinfu.system.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jinfu.common.result.Result;
import com.jinfu.security.annotation.RequiresPermission;
import com.jinfu.system.dto.SysRoleDTO;
import com.jinfu.system.entity.SysRole;
import com.jinfu.system.service.SysRoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/system/role")
@RequiredArgsConstructor
@Tag(name = "Role Management", description = "Role CRUD and role-menu assignment")
public class SysRoleController {

    private final SysRoleService sysRoleService;

    @GetMapping("/list")
    @RequiresPermission("system:role:list")
    @Operation(summary = "Paginated role list", description = "Query roles with filters (roleName, roleKey, status)")
    public Result<IPage<SysRole>> list(
            @Parameter(description = "Page number") @RequestParam(defaultValue = "1") int pageNum,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int pageSize,
            SysRole role) {
        Page<SysRole> page = new Page<>(pageNum, pageSize);
        IPage<SysRole> result = sysRoleService.selectPage(page, role);
        return Result.success(result);
    }

    @GetMapping("/all")
    @Operation(summary = "All active roles", description = "Returns all active roles for dropdown (no pagination)")
    public Result<List<SysRole>> all() {
        return Result.success(sysRoleService.selectRoleAll());
    }

    @GetMapping("/{roleId}")
    @RequiresPermission("system:role:list")
    @Operation(summary = "Get role by ID")
    public Result<SysRole> getInfo(
            @Parameter(description = "Role ID") @PathVariable Long roleId) {
        SysRole role = sysRoleService.getById(roleId);
        return Result.success(role);
    }

    @GetMapping("/menuIds/{roleId}")
    @RequiresPermission("system:role:list")
    @Operation(summary = "Get menu IDs assigned to role")
    public Result<List<Long>> getMenuIds(
            @Parameter(description = "Role ID") @PathVariable Long roleId) {
        return Result.success(sysRoleService.selectMenuIdsByRoleId(roleId));
    }

    @PostMapping
    @RequiresPermission("system:role:add")
    @Operation(summary = "Add role", description = "Create a role together with its role-menu relations")
    public Result<Void> add(@Valid @RequestBody SysRoleDTO roleDTO) {
        sysRoleService.insertRole(roleDTO);
        return Result.success();
    }

    @PutMapping
    @RequiresPermission("system:role:edit")
    @Operation(summary = "Update role", description = "Update a role and refresh its role-menu relations")
    public Result<Void> edit(@Valid @RequestBody SysRoleDTO roleDTO) {
        sysRoleService.updateRole(roleDTO);
        return Result.success();
    }

    @DeleteMapping("/{roleId}")
    @RequiresPermission("system:role:del")
    @Operation(summary = "Delete role", description = "Delete a role together with its role-menu relations")
    public Result<Void> remove(
            @Parameter(description = "Role ID") @PathVariable Long roleId) {
        sysRoleService.deleteRole(roleId);
        return Result.success();
    }
}
