package com.jinfu.daily.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jinfu.common.result.Result;
import com.jinfu.daily.dto.DailyConfigVO;
import com.jinfu.daily.entity.DailyFormConfig;
import com.jinfu.daily.service.DailyFormConfigService;
import com.jinfu.security.annotation.RequiresPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 部门日报配置管理
 * 需求1：每个部门可绑定不同的日报表单（可再绑定审批模板）
 */
@RestController
@RequestMapping("/daily/config")
@RequiredArgsConstructor
@Tag(name = "部门日报配置")
public class DailyConfigController {

    private final DailyFormConfigService configService;

    @GetMapping("/list")
    @RequiresPermission("daily:config:list")
    @Operation(summary = "日报配置分页列表")
    public Result<IPage<DailyConfigVO>> page(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String keyword) {
        Page<DailyFormConfig> page = new Page<>(pageNum, pageSize);
        return Result.success(configService.pageConfigs(page, keyword));
    }

    @GetMapping("/all")
    @RequiresPermission("daily:config:list")
    @Operation(summary = "全部日报配置（含名称，供下拉选择）")
    public Result<List<DailyConfigVO>> all() {
        return Result.success(configService.listConfigs());
    }

    @PostMapping
    @RequiresPermission("daily:config:add")
    @Operation(summary = "新增日报配置（同一部门唯一）")
    public Result<Void> add(@RequestBody DailyFormConfig config) {
        configService.addConfig(config);
        return Result.success();
    }

    @PutMapping("/{id}")
    @RequiresPermission("daily:config:edit")
    @Operation(summary = "修改日报配置")
    public Result<Void> update(@PathVariable Long id, @RequestBody DailyFormConfig config) {
        config.setId(id);
        configService.updateConfig(config);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @RequiresPermission("daily:config:del")
    @Operation(summary = "删除日报配置")
    public Result<Void> remove(@PathVariable Long id) {
        configService.removeConfig(id);
        return Result.success();
    }
}
