package com.jinfu.approval.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jinfu.approval.dto.CompleteApprovalRequest;
import com.jinfu.approval.dto.ProcessInstanceVO;
import com.jinfu.approval.entity.SysProcessInstance;
import com.jinfu.approval.service.ProcessInstanceService;
import com.jinfu.common.result.Result;
import com.jinfu.security.annotation.RequiresPermission;
import com.jinfu.security.entity.LoginUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/approval")
@RequiredArgsConstructor
@Tag(name = "审批操作")
public class ApprovalController {

    private final ProcessInstanceService instanceService;

    @GetMapping("/todo")
    @RequiresPermission("approval:todo:list")
    @Operation(summary = "待我审批列表")
    public Result<IPage<ProcessInstanceVO>> todoList(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @AuthenticationPrincipal LoginUser loginUser) {
        Page<SysProcessInstance> page = new Page<>(pageNum, pageSize);
        return Result.success(instanceService.todoApprovals(page, loginUser.getUserId()));
    }

    @GetMapping("/done")
    @RequiresPermission("approval:todo:list")
    @Operation(summary = "我已审批列表")
    public Result<IPage<ProcessInstanceVO>> doneList(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @AuthenticationPrincipal LoginUser loginUser) {
        Page<SysProcessInstance> page = new Page<>(pageNum, pageSize);
        return Result.success(instanceService.doneApprovals(page, loginUser.getUserId()));
    }

    @PostMapping("/complete")
    @Operation(summary = "审批操作（同意/驳回）")
    public Result<Void> completeApproval(
            @Valid @RequestBody CompleteApprovalRequest request,
            @AuthenticationPrincipal LoginUser loginUser) {
        instanceService.completeApproval(request, loginUser.getUserId(), loginUser.getNickname());
        return Result.success();
    }
}
