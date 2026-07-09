package com.kato.pro.langchain.domain.knowledge;

import com.kato.pro.langchain.config.RagProperties;
import com.kato.pro.langchain.domain.rag.QueryRewriter;
import com.kato.pro.langchain.domain.rag.Reranker;
import com.kato.pro.langchain.domain.rag.Retriever;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

/**
 * RAG 门面：rewrite → retrieve → rerank。
 *
 * 调用方：M8 ChatApplicationService.sendMessage 内部拼 prompt 前调。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RagPipeline {

    private final QueryRewriter queryRewriter;
    private final Retriever retriever;
    private final Reranker reranker;
    private final RagProperties properties;

    public List<ScoredChunk> search(String query, Integer topK, Double minScore,
                                     Set<Long> docIds, Set<String> sourceTypes) {
        String rewritten = properties.getQueryRewriter().isEnabled()
                ? queryRewriter.rewrite(query)
                : (query == null ? "" : query);
        log.debug("RAG query: original='{}' rewritten='{}'", query, rewritten);
        List<ScoredChunk> hits = retriever.retrieve(rewritten, topK, minScore, docIds, sourceTypes);
        if (properties.getReranker().isEnabled()) {
            hits = reranker.rerank(rewritten, hits);
        }
        return hits;
    }
}
