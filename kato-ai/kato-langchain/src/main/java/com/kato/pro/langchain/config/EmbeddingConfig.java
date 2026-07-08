package com.kato.pro.langchain.config;

import com.kato.pro.langchain.domain.embedding.EmbeddingModel;
import com.kato.pro.langchain.infrastructure.external.minimax.InMemoryFakeEmbeddingModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Embedding 模型装配。
 * v1: InMemoryFakeEmbeddingModel（沙箱/开发友好）。
 * v2: 替换为 ONNX bge-small-zh-v1.5 或 HTTP 远程模型 — 只需换 bean 实例。
 */
@Slf4j
@Configuration
public class EmbeddingConfig {

    @Bean
    public EmbeddingModel embeddingModel(
            @Value("${minimax.embedding.dimension:512}") int dimension) {
        log.info("Embedding model: InMemoryFakeEmbeddingModel, dim={}", dimension);
        return new InMemoryFakeEmbeddingModel(dimension);
    }
}
