package com.kato.pro.langchain.domain.rag;

import com.kato.pro.langchain.domain.knowledge.ScoredChunk;

import java.util.List;

/**
 * Reranker SPI（v1 = NoOp，v2 = cross-encoder）。
 */
public interface Reranker {

    List<ScoredChunk> rerank(String query, List<ScoredChunk> candidates);
}
