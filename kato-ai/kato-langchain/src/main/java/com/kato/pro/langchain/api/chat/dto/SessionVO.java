package com.kato.pro.langchain.api.chat.dto;

import com.kato.pro.langchain.domain.chat.ChatSession;
import com.kato.pro.langchain.domain.session.SessionStatus;
import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;
import io.swagger.v3.oas.annotations.media.Schema;


/**
 * 会话视图对象（API 返回）。
 * 不暴露 tenantId（前端不需要）。
 * 不暴露 summary 字段（M4 完整版才暴露）。
 */
@Value
@Builder
@Schema(description = "会话视图对象（API 返回）")
public class SessionVO {
    Long id;
    String title;
    SessionStatus status;
    String channel;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;

    public static SessionVO from(ChatSession s) {
        if (s == null) return null;
        return SessionVO.builder()
                .id(s.getId())
                .title(s.getTitle())
                .status(s.getStatus())
                .channel(s.getChannel())
                .createdAt(s.getCreateTime())
                .updatedAt(s.getUpdateTime())
                .build();
    }
}
