package com.jinfu.message.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jinfu.common.result.Result;
import com.jinfu.message.entity.SysMessage;
import com.jinfu.message.service.MessageService;
import com.jinfu.security.annotation.RequiresPermission;
import com.jinfu.security.entity.LoginUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/message")
@RequiredArgsConstructor
@Tag(name = "站内消息")
public class MessageController {

    private final MessageService messageService;

    @GetMapping("/list")
    @RequiresPermission("message:list")
    @Operation(summary = "我的消息列表（可按 readFlag=0 过滤未读）")
    public Result<IPage<SysMessage>> list(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) Integer readFlag,
            @AuthenticationPrincipal LoginUser loginUser) {
        Page<SysMessage> page = new Page<>(pageNum, pageSize);
        return Result.success(messageService.pageMessages(page, loginUser.getUserId(), readFlag));
    }

    @GetMapping("/unread-count")
    @RequiresPermission("message:list")
    @Operation(summary = "未读消息数（顶部铃铛红点）")
    public Result<Map<String, Long>> unreadCount(@AuthenticationPrincipal LoginUser loginUser) {
        return Result.success(Map.of("count", messageService.countUnread(loginUser.getUserId())));
    }

    @PutMapping("/{id}/read")
    @RequiresPermission("message:list")
    @Operation(summary = "标记单条已读")
    public Result<Void> markRead(@PathVariable Long id, @AuthenticationPrincipal LoginUser loginUser) {
        messageService.markRead(id, loginUser.getUserId());
        return Result.success();
    }

    @PutMapping("/read-all")
    @RequiresPermission("message:list")
    @Operation(summary = "全部标记已读")
    public Result<Void> markAllRead(@AuthenticationPrincipal LoginUser loginUser) {
        messageService.markAllRead(loginUser.getUserId());
        return Result.success();
    }
}
