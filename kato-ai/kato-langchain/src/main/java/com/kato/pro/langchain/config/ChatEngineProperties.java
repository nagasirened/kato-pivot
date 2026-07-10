package com.kato.pro.langchain.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * ChatEngine 配置（chat-engine.*）。
 *
 *   - rag-top-k             : RAG topK（默认 5）
 *   - rag-min-score         : RAG 最小分数（默认 0.6）
 *   - model-timeout-ms      : 主对话模型超时（默认 30s）
 *   - sse-timeout-ms        : SSE emitter 超时（默认 30s）
 *   - tool-call-enabled     : 是否启用 tool_call 解析（默认 true）
 */
@Data
@ConfigurationProperties(prefix = "chat-engine")
public class ChatEngineProperties {
    private int ragTopK = 5;
    private double ragMinScore = 0.6;
    private long modelTimeoutMs = 30_000;
    private long sseTimeoutMs = 30_000;
    private boolean toolCallEnabled = true;
}
