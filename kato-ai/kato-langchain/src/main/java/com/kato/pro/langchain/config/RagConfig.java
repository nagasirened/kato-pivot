package com.kato.pro.langchain.config;

import com.kato.pro.langchain.domain.rag.CachingQueryRewriter;
import com.kato.pro.langchain.domain.rag.LlmQueryRewriter;
import com.kato.pro.langchain.domain.rag.QueryRewriter;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * M5 装配：
 *   - QueryRewriter：CachingQueryRewriter 装饰 LlmQueryRewriter
 */
@Configuration
@EnableConfigurationProperties(RagProperties.class)
public class RagConfig {

    @Bean
    public QueryRewriter queryRewriter(LlmQueryRewriter llm, RagProperties props) {
        return new CachingQueryRewriter(llm, props);
    }
}
