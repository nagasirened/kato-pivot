package com.kato.pro.langchain.infrastructure.external.minimax;

import com.kato.pro.langchain.common.exception.ErrorCode;
import com.kato.pro.langchain.common.exception.SystemException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;

/**
 * MiniMax API 客户端（OpenAI 兼容协议）。
 *
 * 责任范围（M2）：
 *   - 组装请求 + 解析响应
 *   - 鉴权 header 注入
 *   - 超时设置
 *   - 错误分类（4xx vs 5xx）
 *
 * **不在 M2 范围**（由 M7 Resilience4j 包装层负责）：
 *   - 重试（429/5xx）
 *   - 限流
 *   - 熔断
 *   - Fallback
 *
 * 沙箱说明：M2 单元测试用 `ExchangeFunction` stub 拦截 HTTP，不发起真实网络请求。
 */
@Slf4j
public class MiniMaxClient {

    private final WebClient webClient;
    private final MiniMaxProperties properties;

    public MiniMaxClient(WebClient webClient, MiniMaxProperties properties) {
        this.webClient = webClient;
        this.properties = properties;
    }

    /**
     * 单轮对话。返回模型文本回复（取 choices[0].message.content）。
     */
    public Mono<String> chatCompletions(String model, String systemPrompt, String userMessage) {
        ChatCompletionRequest req = ChatCompletionRequest.builder()
                .model(model)
                .messages(List.of(
                        ChatMessage.builder().role(ChatMessage.Role.SYSTEM).content(systemPrompt).build(),
                        ChatMessage.builder().role(ChatMessage.Role.USER).content(userMessage).build()))
                .stream(false)
                .maxTokens(1024)
                .build();
        return chatCompletions(req).map(resp -> {
            if (resp.getChoices() == null || resp.getChoices().isEmpty()) {
                throw new SystemException(ErrorCode.UPSTREAM_ERROR,
                        "MiniMax returned empty choices for model " + model);
            }
            return resp.getChoices().get(0).getMessage().getContent();
        });
    }

    /**
     * 完整请求体调用。供 M3+ 工具调用决策使用。
     */
    public Mono<ChatCompletionResponse> chatCompletions(ChatCompletionRequest request) {
        return webClient.post()
                .uri("/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getApiKey())
                .bodyValue(request)
                .retrieve()
                .bodyToMono(ChatCompletionResponse.class)
                .timeout(Duration.ofMillis(properties.getReadTimeoutMs()))
                .doOnError(WebClientResponseException.class, e -> log.error(
                        "MiniMax API error: status={}, body={}", e.getStatusCode(), e.getResponseBodyAsString()))
                .onErrorMap(WebClientResponseException.class, e -> new SystemException(
                        ErrorCode.UPSTREAM_ERROR,
                        "MiniMax API error: HTTP " + e.getStatusCode().value()));
    }
}
