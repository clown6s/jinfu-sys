package com.jinfu.flowable.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jinfu.common.result.Result;
import com.jinfu.flowable.dto.ProcessDesignSaveRequest;
import com.jinfu.flowable.dto.ProcessDesignVO;
import com.jinfu.flowable.entity.ProcessDesign;
import com.jinfu.flowable.service.ProcessDesignService;
import com.jinfu.security.annotation.RequiresPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 流程设计草稿管理 — 设计器画布持久化 + 一键发布到 Flowable
 */
@RestController
@RequestMapping("/flow/design")
@RequiredArgsConstructor
@Tag(name = "Process Design", description = "Process design draft management APIs")
public class ProcessDesignController {

    private final ProcessDesignService designService;

    @GetMapping("/list")
    @RequiresPermission("flow:design:list")
    @Operation(summary = "Paginated process design list")
    public Result<IPage<ProcessDesignVO>> page(
            @Parameter(description = "Page number") @RequestParam(defaultValue = "1") int pageNum,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int pageSize,
            @Parameter(description = "Search keyword (name/key)") @RequestParam(required = false) String keyword,
            @Parameter(description = "Status: 0=draft 1=published") @RequestParam(required = false) Integer status) {
        Page<ProcessDesign> page = new Page<>(pageNum, pageSize);
        return Result.success(designService.pageDesigns(page, keyword, status));
    }

    @GetMapping("/{id}")
    @RequiresPermission("flow:design:list")
    @Operation(summary = "Process design detail (with BPMN XML)")
    public Result<ProcessDesign> detail(@PathVariable Long id) {
        return Result.success(designService.getDetail(id));
    }

    @PostMapping
    @RequiresPermission("flow:design:save")
    @Operation(summary = "Save process design", description = "Create when id is absent, update otherwise. Returns design id.")
    public Result<Map<String, Long>> save(@Valid @RequestBody ProcessDesignSaveRequest request) {
        Long id = designService.saveDesign(request);
        Map<String, Long> result = new HashMap<>();
        result.put("id", id);
        return Result.success(result);
    }

    @DeleteMapping("/{id}")
    @RequiresPermission("flow:design:del")
    @Operation(summary = "Delete process design draft")
    public Result<Void> remove(@PathVariable Long id) {
        designService.removeDesign(id);
        return Result.success();
    }

    @PostMapping("/{id}/publish")
    @RequiresPermission("flow:design:publish")
    @Operation(summary = "Publish process design", description = "Deploy to Flowable engine; same key auto-increments version")
    public Result<Void> publish(@PathVariable Long id) {
        designService.publishDesign(id);
        return Result.success();
    }
}
