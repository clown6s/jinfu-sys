package com.jinfu.approval.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jinfu.approval.dto.ProcessInstanceVO;
import com.jinfu.approval.dto.StartProcessRequest;
import com.jinfu.approval.entity.SysProcessInstance;
import com.jinfu.approval.service.ProcessInstanceService;
import com.jinfu.common.result.Result;
import com.jinfu.common.result.ResultCode;
import com.jinfu.security.annotation.RequiresPermission;
import com.jinfu.security.entity.LoginUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/approval/instance")
@RequiredArgsConstructor
@Tag(name = "审批实例管理")
public class ProcessInstanceController {

    private final ProcessInstanceService instanceService;

    @PostMapping("/start")
    @Operation(summary = "发起审批")
    public Result<ProcessInstanceVO> startProcess(
            @Valid @RequestBody StartProcessRequest request,
            @AuthenticationPrincipal LoginUser loginUser) {
        ProcessInstanceVO vo = instanceService.startProcess(
                request, loginUser.getUserId(), loginUser.getNickname(), loginUser.getDeptId());
        return Result.success(vo);
    }

    @GetMapping("/my")
    @RequiresPermission("approval:my:list")
    @Operation(summary = "我的申请列表")
    public Result<IPage<ProcessInstanceVO>> myApplications(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @AuthenticationPrincipal LoginUser loginUser) {
        Page<SysProcessInstance> page = new Page<>(pageNum, pageSize);
        return Result.success(instanceService.myApplications(page, loginUser.getUserId()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "审批详情")
    public Result<ProcessInstanceVO> getDetail(@PathVariable Long id) {
        return Result.success(instanceService.getDetail(id));
    }

    @PutMapping("/{id}/cancel")
    @Operation(summary = "撤销审批")
    public Result<Void> cancel(@PathVariable Long id, @AuthenticationPrincipal LoginUser loginUser) {
        instanceService.cancelProcess(id, loginUser.getUserId());
        return Result.success();
    }
}
