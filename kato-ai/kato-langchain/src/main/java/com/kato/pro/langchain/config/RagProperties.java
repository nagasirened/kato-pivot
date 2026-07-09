package com.kato.pro.langchain.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * RAG 流水线配置。
 */
@Data
@ConfigurationProperties(prefix = "rag")
public class RagProperties {

    private Retriever retriever = new Retriever();
    private Reranker reranker = new Reranker();
    private QueryRewriter queryRewriter = new QueryRewriter();

    @Data
    public static class Retriever {
        private int topK = 5;
        private double minScore = 0.6;
    }

    @Data
    public static class Reranker {
        /** v1: false = NoOpReranker；v2 可启用 cross-encoder */
        private boolean enabled = false;
    }

    @Data
    public static class QueryRewriter {
        private boolean enabled = true;
        private long cacheTtlSeconds = 86_400L;
    }
}
