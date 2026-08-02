package com.jinfu.daily.controller;

import com.jinfu.common.result.Result;
import com.jinfu.daily.entity.LogType;
import com.jinfu.daily.service.LogTypeService;
import com.jinfu.security.annotation.RequiresPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 日志类型管理 — 日报/周报/月报/项目日志等类型定义
 */
@RestController
@RequestMapping("/log-type")
@RequiredArgsConstructor
@Tag(name = "日志类型管理")
public class LogTypeController {

    private final LogTypeService logTypeService;

    @GetMapping("/list")
    @RequiresPermission("log-type:list")
    @Operation(summary = "日志类型列表（全部）")
    public Result<List<LogType>> list() {
        return Result.success(logTypeService.list());
    }

    @GetMapping("/enabled")
    @Operation(summary = "启用的日志类型列表（供下拉选择）")
    public Result<List<LogType>> enabled() {
        return Result.success(logTypeService.listEnabled());
    }

    @PostMapping
    @RequiresPermission("log-type:add")
    @Operation(summary = "新增日志类型")
    public Result<Void> add(@Valid @RequestBody LogType logType) {
        logTypeService.save(logType);
        return Result.success();
    }

    @PutMapping("/{id}")
    @RequiresPermission("log-type:edit")
    @Operation(summary = "修改日志类型")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody LogType logType) {
        logType.setId(id);
        logTypeService.updateById(logType);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @RequiresPermission("log-type:del")
    @Operation(summary = "删除日志类型")
    public Result<Void> remove(@PathVariable Long id) {
        logTypeService.removeById(id);
        return Result.success();
    }
}
