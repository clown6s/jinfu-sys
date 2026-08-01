package com.jinfu.system.controller;

import com.jinfu.common.result.Result;
import com.jinfu.security.annotation.RequiresPermission;
import com.jinfu.system.entity.SysDept;
import com.jinfu.system.service.SysDeptService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/system/dept")
@RequiredArgsConstructor
@Tag(name = "Department Management", description = "Department CRUD, tree query")
public class SysDeptController {

    private final SysDeptService sysDeptService;

    @GetMapping("/list")
    @RequiresPermission("system:dept:list")
    @Operation(summary = "Department tree list", description = "Query departments as a tree structure")
    public Result<List<SysDept>> list(
            @RequestParam(required = false) String deptName,
            @RequestParam(required = false) Integer status) {
        SysDept filter = new SysDept();
        filter.setDeptName(deptName);
        filter.setStatus(status);
        return Result.success(sysDeptService.selectDeptTree(filter));
    }

    @GetMapping("/{deptId}")
    @RequiresPermission("system:dept:list")
    @Operation(summary = "Get department by ID")
    public Result<SysDept> getById(@PathVariable Long deptId) {
        return Result.success(sysDeptService.getById(deptId));
    }

    @PostMapping
    @RequiresPermission("system:dept:add")
    @Operation(summary = "Add department")
    public Result<Void> add(@Valid @RequestBody SysDept dept) {
        sysDeptService.insertDept(dept);
        return Result.success();
    }

    @PutMapping
    @RequiresPermission("system:dept:edit")
    @Operation(summary = "Update department")
    public Result<Void> update(@Valid @RequestBody SysDept dept) {
        sysDeptService.updateDept(dept);
        return Result.success();
    }

    @DeleteMapping("/{deptId}")
    @RequiresPermission("system:dept:del")
    @Operation(summary = "Delete department")
    public Result<Void> delete(@PathVariable Long deptId) {
        sysDeptService.deleteDept(deptId);
        return Result.success();
    }
}
