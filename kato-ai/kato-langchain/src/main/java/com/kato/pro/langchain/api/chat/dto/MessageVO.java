package com.kato.pro.langchain.api.chat.dto;

import com.kato.pro.langchain.domain.chat.ChatMessage;
import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;
import io.swagger.v3.oas.annotations.media.Schema;


/**
 * 消息视图对象。
 *
 * v1 不暴露 toolCallJson / citationDocIds（前端 v1 不展示；调试时可通过 admin API 查）。
 * v2 会增加这些字段。
 */
@Value
@Builder
@Schema(description = "消息视图对象")
public class MessageVO {
    Long id;
    Long sessionId;
    ChatMessage.Role role;
    String content;
    String modelName;
    Integer tokenCount;
    LocalDateTime createdAt;

    public static MessageVO from(ChatMessage m) {
        if (m == null) return null;
        return MessageVO.builder()
                .id(m.getId())
                .sessionId(m.getSessionId())
                .role(m.getRole())
                .content(m.getContent())
                .modelName(m.getModelName())
                .tokenCount(m.getTokenCount())
                .createdAt(m.getCreateTime())
                .build();
    }
}
