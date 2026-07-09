package com.kato.pro.langchain.domain.rag;

import com.kato.pro.langchain.domain.knowledge.ScoredChunk;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 默认 NoOp 实现：按原序返回（Retriever 已按 cosine 排序）。
 */
@Component
public class NoOpReranker implements Reranker {

    @Override
    public List<ScoredChunk> rerank(String query, List<ScoredChunk> candidates) {
        return candidates;
    }
}
