package com.kato.pro.langchain.domain.rag;

import com.kato.pro.langchain.domain.knowledge.ScoredChunk;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class NoOpRerankerTest {

    @Test
    void rerank_returnsInputUnchanged() {
        NoOpReranker r = new NoOpReranker();
        List<ScoredChunk> in = List.of(
                new ScoredChunk(1L, 10L, "a", 0.9, java.util.Map.of()),
                new ScoredChunk(2L, 10L, "b", 0.7, java.util.Map.of()));
        assertSame(in, r.rerank("q", in));
    }

    @Test
    void rerank_empty_returnsEmpty() {
        assertEquals(List.of(), new NoOpReranker().rerank("q", List.of()));
    }
}
