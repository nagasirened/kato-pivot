package com.kato.pro.langchain.infrastructure.external.minimax;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Builder;
import lombok.Value;

/**
 * OpenAI 兼容消息结构。
 * role: system / user / assistant / tool
 */
@Value
@Builder
public class ChatMessage {

    public enum Role {
        SYSTEM, USER, ASSISTANT, TOOL;

        /** Jackson 序列化：输出小写（OpenAI 协议格式） */
        @JsonValue
        public String toJson() {
            return name().toLowerCase();
        }

        /** Jackson 反序列化：接受小写（OpenAI 协议格式） */
        @JsonCreator
        public static Role fromJson(String value) {
            if (value == null) return null;
            return Role.valueOf(value.toUpperCase());
        }
    }

    Role role;
    String content;
}
