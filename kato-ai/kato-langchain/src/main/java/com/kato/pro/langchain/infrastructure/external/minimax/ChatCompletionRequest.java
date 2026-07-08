package com.kato.pro.langchain.infrastructure.external.minimax;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;

import java.util.List;

/**
 * OpenAI 兼容的 /v1/chat/completions 请求体。
 * 字段命名严格遵循 OpenAI 协议（snake_case via @JsonProperty）。
 */
@Value
@Builder
public class ChatCompletionRequest {

    String model;

    List<ChatMessage> messages;

    @JsonProperty("max_tokens")
    @Builder.Default
    Integer maxTokens = 1024;

    Double temperature;

    /** 是否流式响应（SSE）。M2 走非流式，M3+ 再加流式。 */
    @Builder.Default
    Boolean stream = false;
}
