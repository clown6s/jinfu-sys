package com.jinfu.message.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.jinfu.message.entity.SysMessage;

public interface MessageService extends IService<SysMessage> {

    /** 消息类型常量 */
    String TYPE_APPROVAL = "approval";
    String TYPE_CC = "cc";
    String TYPE_DAILY = "daily";
    String TYPE_SYSTEM = "system";

    /**
     * 发送站内消息：落库 + WebSocket 实时推送（离线也不丢）
     */
    void sendToUser(Long userId, String msgType, String title, String content, Long bizId);

    /**
     * 分页查询我的消息（可按已读/未读过滤）
     */
    IPage<SysMessage> pageMessages(Page<SysMessage> page, Long userId, Integer readFlag);

    /**
     * 未读消息数（顶部铃铛红点）
     */
    long countUnread(Long userId);

    /**
     * 标记单条已读
     */
    void markRead(Long id, Long userId);

    /**
     * 全部标记已读
     */
    void markAllRead(Long userId);
}
