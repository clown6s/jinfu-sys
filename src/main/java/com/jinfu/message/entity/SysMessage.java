package com.jinfu.message.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 站内消息（落库 + WebSocket 实时推送双写）
 */
@Data
@TableName("sys_message")
public class SysMessage {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 接收用户ID */
    private Long userId;

    /** approval=审批 cc=抄送 daily=日报 system=系统 */
    private String msgType;

    /** 消息标题 */
    private String title;

    /** 消息内容 */
    private String content;

    /** 关联业务ID（审批实例ID等） */
    private Long bizId;

    /** 0=未读 1=已读 */
    private Integer readFlag;

    /** 阅读时间 */
    private LocalDateTime readTime;

    private LocalDateTime createTime;
}
