package com.jinfu.message.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jinfu.common.exception.BusinessException;
import com.jinfu.common.result.ResultCode;
import com.jinfu.message.entity.SysMessage;
import com.jinfu.message.mapper.SysMessageMapper;
import com.jinfu.message.service.MessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessageServiceImpl
        extends ServiceImpl<SysMessageMapper, SysMessage>
        implements MessageService {

    private final SimpMessagingTemplate messagingTemplate;

    @Override
    public void sendToUser(Long userId, String msgType, String title, String content, Long bizId) {
        // 1. 落库（离线/刷新页面后仍可查）
        SysMessage message = new SysMessage();
        message.setUserId(userId);
        message.setMsgType(msgType);
        message.setTitle(title);
        message.setContent(content);
        message.setBizId(bizId);
        message.setReadFlag(0);
        message.setCreateTime(LocalDateTime.now());
        save(message);

        // 2. WebSocket 实时推送
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("id", message.getId());
            payload.put("type", msgType);
            payload.put("title", title);
            payload.put("content", content);
            payload.put("bizId", bizId);
            payload.put("readFlag", 0);
            payload.put("timestamp", System.currentTimeMillis());
            messagingTemplate.convertAndSendToUser(
                    String.valueOf(userId), "/queue/notifications", payload);
        } catch (Exception e) {
            log.error("WebSocket 推送失败: userId={}, title={}", userId, title, e);
        }
    }

    @Override
    public IPage<SysMessage> pageMessages(Page<SysMessage> page, Long userId, Integer readFlag) {
        LambdaQueryWrapper<SysMessage> wrapper = new LambdaQueryWrapper<SysMessage>()
                .eq(SysMessage::getUserId, userId)
                .eq(readFlag != null, SysMessage::getReadFlag, readFlag)
                .orderByDesc(SysMessage::getCreateTime);
        return page(page, wrapper);
    }

    @Override
    public long countUnread(Long userId) {
        return count(new LambdaQueryWrapper<SysMessage>()
                .eq(SysMessage::getUserId, userId)
                .eq(SysMessage::getReadFlag, 0));
    }

    @Override
    public void markRead(Long id, Long userId) {
        SysMessage message = getById(id);
        if (message == null || !userId.equals(message.getUserId())) {
            throw new BusinessException(ResultCode.DATA_NOT_EXIST, "消息不存在");
        }
        if (message.getReadFlag() == 1) {
            return;
        }
        message.setReadFlag(1);
        message.setReadTime(LocalDateTime.now());
        updateById(message);
    }

    @Override
    public void markAllRead(Long userId) {
        SysMessage update = new SysMessage();
        update.setReadFlag(1);
        update.setReadTime(LocalDateTime.now());
        update(update, new LambdaQueryWrapper<SysMessage>()
                .eq(SysMessage::getUserId, userId)
                .eq(SysMessage::getReadFlag, 0));
    }
}
