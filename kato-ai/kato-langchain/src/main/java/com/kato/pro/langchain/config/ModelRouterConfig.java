package com.kato.pro.langchain.config;

import com.kato.pro.langchain.common.tenant.TenantContext;
import com.kato.pro.langchain.domain.chat.ChatLanguageModel;
import com.kato.pro.langchain.domain.chat.ModelProperties;
import com.kato.pro.langchain.domain.chat.ModelRouter;
import com.kato.pro.langchain.domain.chat.TaskType;
import com.kato.pro.langchain.infrastructure.external.minimax.MiniMaxClient;
import com.kato.pro.langchain.infrastructure.external.minimax.MiniMaxProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.HashMap;
import java.util.Map;

/**
 * Model 层 Bean 装配：
 *   - WebClient (with baseUrl + Authorization header)
 *   - MiniMaxClient
 *   - ChatLanguageModel 适配器（按 model name 路由）
 *   - ModelRouter（按 TaskType 路由）
 */
@Slf4j
@Configuration
@EnableConfigurationProperties({MiniMaxProperties.class, ModelProperties.class})
public class ModelRouterConfig {

    @Bean
    public WebClient miniMaxWebClient(MiniMaxProperties properties) {
        return WebClient.builder()
                .baseUrl(properties.getBaseUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getApiKey())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .codecs(c -> c.defaultCodecs().maxInMemorySize(10 * 1024 * 1024)) // 10MB
                .build();
    }

    @Bean
    public MiniMaxClient miniMaxClient(WebClient miniMaxWebClient, MiniMaxProperties properties) {
        return new MiniMaxClient(miniMaxWebClient, properties);
    }

    @Bean
    public ModelRouter modelRouter(MiniMaxClient client, ModelProperties modelProps) {
        ChatLanguageModel simpleAdapter = new MiniMaxChatModelAdapter(client, modelProps.getSimple());
        ChatLanguageModel complexAdapter = new MiniMaxChatModelAdapter(client, modelProps.getComplex());

        Map<TaskType, ChatLanguageModel> table = new HashMap<>();
        // 简单任务 → m2.5
        table.put(TaskType.SIMPLE_CLASSIFICATION, simpleAdapter);
        table.put(TaskType.SIMPLE_SUMMARIZATION, simpleAdapter);
        table.put(TaskType.SIMPLE_QUERY_REWRITE, simpleAdapter);
        // 复杂任务 → m3
        table.put(TaskType.COMPLEX_CHAT, complexAdapter);
        table.put(TaskType.COMPLEX_TOOL_CALL_DECISION, complexAdapter);
        // EMBEDDING 不进 router（走 EmbeddingModel）

        log.info("ModelRouter configured: simple={}, complex={}", modelProps.getSimple(), modelProps.getComplex());
        return new ModelRouter(table);
    }

    /**
     * 把 MiniMaxClient 适配成 ChatLanguageModel 接口。
     */
    public static class MiniMaxChatModelAdapter implements ChatLanguageModel {
        private final MiniMaxClient client;
        private final String modelName;
        MiniMaxChatModelAdapter(MiniMaxClient client, String modelName) {
            this.client = client;
            this.modelName = modelName;
        }
        @Override public String modelName() { return modelName; }
        @Override
        public reactor.core.publisher.Mono<String> chat(String systemPrompt, String userMessage) {
            log.debug("Chat call: model={}, tenant={}", modelName,
                    TenantContext.currentOrNull() != null ? TenantContext.currentOrNull().tenantId() : "n/a");
            return client.chatCompletions(modelName, systemPrompt, userMessage);
        }
    }
}
