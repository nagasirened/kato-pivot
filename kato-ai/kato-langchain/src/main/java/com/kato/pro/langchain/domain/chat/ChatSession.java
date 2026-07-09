package com.kato.pro.langchain.domain.chat;

import com.baomidou.mybatisplus.annotation.TableName;
import com.kato.pro.langchain.common.entity.BaseEntity;
import com.kato.pro.langchain.domain.session.SessionStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 聊天会话实体。一个会话代表一个完整的对话线程（1 user ↔ N messages）。
 *
 * 字段语义：
 *   - title: 用户给的名 / 系统根据首条消息自动生成（M8 ChatEngine 写入）
 *   - summary: M4 HistorySummarizer 维护的滚动摘要
 *   - summaryVersion: 自增，每次摘要更新 +1
 *   - channel: 渠道预留（v2 启用，区分网页/小程序/微信）
 *   - status: ACTIVE / ARCHIVED
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("chat_session")
public class ChatSession extends BaseEntity {

    /** 会话所有者（来自 TenantContext.currentUserId） */
    private Long userId;

    /** 渠道：v1 固定 "DEFAULT"，v2 启用（web / miniapp / wechat 等） */
    private String channel;

    /** 会话标题（可空，懒生成） */
    private String title;

    /** 滚动摘要（M4 写入） */
    private String summary;

    /** 摘要版本号（自增） */
    private Integer summaryVersion;

    /** 状态：ACTIVE / ARCHIVED */
    private SessionStatus status;
}
