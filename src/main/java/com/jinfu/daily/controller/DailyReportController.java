package com.jinfu.daily.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jinfu.common.result.Result;
import com.jinfu.daily.dto.DailyReportVO;
import com.jinfu.daily.dto.DailySubmitRequest;
import com.jinfu.daily.entity.DailyReport;
import com.jinfu.daily.service.DailyReportService;
import com.jinfu.security.annotation.RequiresPermission;
import com.jinfu.security.entity.LoginUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 日志填报 — 支持日报/周报/月报/项目日志等多种类型
 */
@RestController
@RequestMapping("/daily")
@RequiredArgsConstructor
@Tag(name = "日志填报")
public class DailyReportController {

    private final DailyReportService reportService;

    @GetMapping("/my-form")
    @RequiresPermission("daily:report:list")
    @Operation(summary = "我的日志表单（返回部门配置的表单 Schema 与今日提交状态）")
    public Result<DailyReportVO> myForm(
            @RequestParam Long logTypeId,
            @AuthenticationPrincipal LoginUser loginUser) {
        return Result.success(reportService.myForm(
                loginUser.getUserId(), loginUser.getDeptId(), logTypeId));
    }

    @PostMapping("/submit")
    @RequiresPermission("daily:report:add")
    @Operation(summary = "提交日志（配置了审批模板则自动发起审批）")
    public Result<DailyReportVO> submit(
            @Valid @RequestBody DailySubmitRequest request,
            @AuthenticationPrincipal LoginUser loginUser) {
        return Result.success(reportService.submit(
                request, loginUser.getUserId(), loginUser.getNickname(), loginUser.getDeptId(), request.getLogTypeId()));
    }

    @GetMapping("/my")
    @RequiresPermission("daily:report:list")
    @Operation(summary = "我的日志历史（可按类型筛选）")
    public Result<IPage<DailyReportVO>> myReports(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) Long logTypeId,
            @AuthenticationPrincipal LoginUser loginUser) {
        Page<DailyReport> page = new Page<>(pageNum, pageSize);
        return Result.success(reportService.myReports(page, loginUser.getUserId(), logTypeId));
    }

    @GetMapping("/{id}")
    @RequiresPermission("daily:report:list")
    @Operation(summary = "日志详情")
    public Result<DailyReportVO> detail(@PathVariable Long id, @AuthenticationPrincipal LoginUser loginUser) {
        return Result.success(reportService.detail(id, loginUser.getUserId()));
    }
}
