package com.kato.pro.langchain.infrastructure.external.minimax;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Value;

import java.util.List;

/**
 * OpenAI 兼容的 /v1/chat/completions 响应体。
 * 完整字段在生产用得到；M2 只取必要字段。
 */
@Value
public class ChatCompletionResponse {

    String id;
    String model;
    List<Choice> choices;

    Usage usage;

    @Value
    public static class Choice {
        Integer index;
        ChatMessage message;
        @JsonProperty("finish_reason")
        String finishReason;
    }

    @Value
    public static class Usage {
        @JsonProperty("prompt_tokens")
        Integer promptTokens;
        @JsonProperty("completion_tokens")
        Integer completionTokens;
        @JsonProperty("total_tokens")
        Integer totalTokens;
    }
}
