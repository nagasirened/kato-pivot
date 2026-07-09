package com.kato.pro.langchain.domain.rag;

import com.kato.pro.langchain.common.tenant.TenantContext;
import com.kato.pro.langchain.config.RagProperties;
import com.kato.pro.langchain.domain.embedding.EmbeddingModel;
import com.kato.pro.langchain.domain.embedding.EmbeddingRequest;
import com.kato.pro.langchain.domain.knowledge.ScoredChunk;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

/**
 * 检索器（D8=C）：
 *   - Embedding query → vector
 *   - VectorStore.search(tenantId, vector, topK, minScore, docIds, sourceTypes)
 *
 * 调用方：RagPipeline.search(...)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class Retriever {

    private final EmbeddingModel embeddingModel;
    private final VectorStore vectorStore;
    private final RagProperties properties;

    public List<ScoredChunk> retrieve(String query, Integer topKOverride, Double minScoreOverride,
                                       Set<Long> docIds, Set<String> sourceTypes) {
        if (query == null || query.isBlank()) return List.of();
        int topK = topKOverride != null && topKOverride > 0
                ? Math.min(topKOverride, 50)
                : properties.getRetriever().getTopK();
        double minScore = minScoreOverride != null
                ? minScoreOverride
                : properties.getRetriever().getMinScore();

        EmbeddingRequest req = EmbeddingRequest.builder().inputs(List.of(query)).build();
        float[] qv = embeddingModel.embed(req).block().getVectors().get(0);
        Long tenantId = TenantContext.currentTenantId();
        return vectorStore.search(tenantId, qv, topK, minScore, docIds, sourceTypes);
    }
}
