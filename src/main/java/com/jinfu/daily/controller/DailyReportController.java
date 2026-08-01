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
 * 日报填报
 * 需求1+2：日报绑定部门表单，提交后自动发起审批、审批结果消息通知
 */
@RestController
@RequestMapping("/daily")
@RequiredArgsConstructor
@Tag(name = "日报填报")
public class DailyReportController {

    private final DailyReportService reportService;

    @GetMapping("/my-form")
    @RequiresPermission("daily:report:list")
    @Operation(summary = "我的日报表单（返回部门配置的表单 Schema 与今日提交状态）")
    public Result<DailyReportVO> myForm(@AuthenticationPrincipal LoginUser loginUser) {
        return Result.success(reportService.myForm(
                loginUser.getUserId(), loginUser.getDeptId()));
    }

    @PostMapping("/submit")
    @RequiresPermission("daily:report:add")
    @Operation(summary = "提交日报（配置了审批模板则自动发起审批）")
    public Result<DailyReportVO> submit(
            @Valid @RequestBody DailySubmitRequest request,
            @AuthenticationPrincipal LoginUser loginUser) {
        return Result.success(reportService.submit(
                request, loginUser.getUserId(), loginUser.getNickname(), loginUser.getDeptId()));
    }

    @GetMapping("/my")
    @RequiresPermission("daily:report:list")
    @Operation(summary = "我的日报历史")
    public Result<IPage<DailyReportVO>> myReports(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @AuthenticationPrincipal LoginUser loginUser) {
        Page<DailyReport> page = new Page<>(pageNum, pageSize);
        return Result.success(reportService.myReports(page, loginUser.getUserId()));
    }

    @GetMapping("/{id}")
    @RequiresPermission("daily:report:list")
    @Operation(summary = "日报详情")
    public Result<DailyReportVO> detail(@PathVariable Long id, @AuthenticationPrincipal LoginUser loginUser) {
        return Result.success(reportService.detail(id, loginUser.getUserId()));
    }
}
