package com.jinfu.system.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jinfu.common.result.Result;
import com.jinfu.security.annotation.RequiresPermission;
import com.jinfu.system.dto.SysUserDTO;
import com.jinfu.system.entity.SysUser;
import com.jinfu.system.service.SysUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.Map;

@RestController
@RequestMapping("/system/user")
@RequiredArgsConstructor
@Tag(name = "User Management", description = "User CRUD, reset password, change status")
public class SysUserController {

    private final SysUserService sysUserService;

    @GetMapping("/list")
    @RequiresPermission("system:user:list")
    @Operation(summary = "Paginated user list", description = "Query users with optional filters")
    public Result<IPage<SysUser>> list(
            @Parameter(description = "Page number") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") Integer pageSize,
            @Parameter(description = "Username (fuzzy)") @RequestParam(required = false) String username,
            @Parameter(description = "Phone (fuzzy)") @RequestParam(required = false) String phone,
            @Parameter(description = "Status") @RequestParam(required = false) Integer status,
            @Parameter(description = "Department ID") @RequestParam(required = false) Long deptId) {
        Page<SysUser> page = new Page<>(pageNum, pageSize);
        SysUser filter = new SysUser();
        filter.setUsername(username);
        filter.setPhone(phone);
        filter.setStatus(status);
        filter.setDeptId(deptId);
        return Result.success(sysUserService.selectPage(page, filter));
    }

    @GetMapping("/{userId}")
    @RequiresPermission("system:user:list")
    @Operation(summary = "Get user by ID")
    public Result<SysUser> getById(@PathVariable Long userId) {
        return Result.success(sysUserService.selectUserById(userId));
    }

    @PostMapping
    @RequiresPermission("system:user:add")
    @Operation(summary = "Add user")
    public Result<Void> add(@Valid @RequestBody SysUserDTO userDTO) {
        sysUserService.insertUser(userDTO);
        return Result.success();
    }

    @PutMapping
    @RequiresPermission("system:user:edit")
    @Operation(summary = "Update user")
    public Result<Void> update(@Valid @RequestBody SysUserDTO userDTO) {
        sysUserService.updateUser(userDTO);
        return Result.success();
    }

    @DeleteMapping("/{userIds}")
    @RequiresPermission("system:user:del")
    @Operation(summary = "Delete users", description = "Delete by IDs, separate multiple with comma")
    public Result<Void> delete(@PathVariable String userIds) {
        Arrays.stream(userIds.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Long::parseLong)
                .forEach(sysUserService::deleteUser);
        return Result.success();
    }

    @PutMapping("/resetPassword")
    @RequiresPermission("system:user:edit")
    @Operation(summary = "Reset user password")
    public Result<Void> resetPassword(@RequestBody Map<String, Object> params) {
        Long userId = Long.valueOf(params.get("userId").toString());
        String password = params.get("password").toString();
        sysUserService.resetPassword(userId, password);
        return Result.success();
    }

    @PutMapping("/changeStatus")
    @RequiresPermission("system:user:edit")
    @Operation(summary = "Change user status")
    public Result<Void> changeStatus(@RequestBody Map<String, Object> params) {
        Long userId = Long.valueOf(params.get("userId").toString());
        Integer status = Integer.valueOf(params.get("status").toString());
        sysUserService.changeStatus(userId, status);
        return Result.success();
    }
}
