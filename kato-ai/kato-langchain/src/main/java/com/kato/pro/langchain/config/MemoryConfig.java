package com.kato.pro.langchain.config;

import com.kato.pro.langchain.domain.chat.ModelRouter;
import com.kato.pro.langchain.domain.memory.HistorySummarizer;
import com.kato.pro.langchain.domain.memory.MemoryManager;
import com.kato.pro.langchain.domain.memory.MemoryWindow;
import com.kato.pro.langchain.domain.memory.TokenCountEstimator;
import com.kato.pro.langchain.domain.session.ChatMessageService;
import com.kato.pro.langchain.domain.session.ChatSessionService;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * M4 记忆管理装配。
 *
 * 装配策略：
 *   - TokenCountEstimator 默认 chars/4 fallback（沙箱里 OpenAiTokenizer 不在 m2；生产可换为
 *     OpenAiTokenizer / HuggingFaceTokenizer 等）。
 *   - HistorySummarizer / MemoryManager 注入以上所有依赖。
 */
@Configuration
@EnableConfigurationProperties(MemoryProperties.class)
public class MemoryConfig {

    /**
     * 默认纯 chars/4 fallback estimator。
     * 业务方若有更精确的 tokenizer 可在配置中注入自己的 TokenCountEstimator Bean 覆盖。
     */
    @Bean
    public TokenCountEstimator tokenCountEstimator() {
        return new TokenCountEstimator(null);
    }

    @Bean
    public MemoryWindow memoryWindow() {
        return new MemoryWindow();
    }

    @Bean
    public HistorySummarizer historySummarizer(ModelRouter modelRouter,
                                               ChatSessionService sessionService,
                                               TokenCountEstimator estimator,
                                               MemoryProperties props) {
        return new HistorySummarizer(modelRouter, sessionService, estimator, props);
    }

    @Bean
    public MemoryManager memoryManager(ChatSessionService sessionService,
                                       ChatMessageService messageService,
                                       TokenCountEstimator estimator,
                                       MemoryWindow window,
                                       HistorySummarizer summarizer,
                                       MemoryProperties props) {
        return new MemoryManager(sessionService, messageService, estimator, window, summarizer, props);
    }
}
