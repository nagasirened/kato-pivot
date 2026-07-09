package com.kato.pro.langchain.domain.knowledge;

import com.kato.pro.langchain.config.RagProperties;
import com.kato.pro.langchain.domain.rag.QueryRewriter;
import com.kato.pro.langchain.domain.rag.Reranker;
import com.kato.pro.langchain.domain.rag.Retriever;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RagPipelineTest {

    @Test
    void search_rewriterDisabled_skipsRewrite() {
        RagProperties props = new RagProperties();
        props.getQueryRewriter().setEnabled(false);
        props.getReranker().setEnabled(false);

        QueryRewriter rw = q -> { throw new AssertionError("should not be called"); };
        Retriever ret = new Retriever(null, null, props) {
            @Override
            public List<ScoredChunk> retrieve(String query, Integer t, Double m,
                                               Set<Long> d, Set<String> s) {
                return List.of(new ScoredChunk(1L, 10L, "x", 0.9, java.util.Map.of()));
            }
        };
        Reranker rk = (q, c) -> c;

        RagPipeline p = new RagPipeline(rw, ret, rk, props);
        List<ScoredChunk> hits = p.search("hello", null, null, null, null);
        assertEquals(1, hits.size());
    }

    @Test
    void search_rewriterEnabled_callsRewriter() {
        RagProperties props = new RagProperties();
        props.getQueryRewriter().setEnabled(true);
        props.getReranker().setEnabled(false);

        java.util.concurrent.atomic.AtomicReference<String> seen = new java.util.concurrent.atomic.AtomicReference<>();
        QueryRewriter rw = q -> { seen.set(q); return "rewritten:" + q; };
        Retriever ret = new Retriever(null, null, props) {
            @Override
            public List<ScoredChunk> retrieve(String query, Integer t, Double m,
                                               Set<Long> d, Set<String> s) {
                assertEquals("rewritten:hello", query);
                return List.of();
            }
        };

        RagPipeline p = new RagPipeline(rw, ret, (q, c) -> c, props);
        p.search("hello", null, null, null, null);
        assertEquals("hello", seen.get());
    }

    @Test
    void search_rerankerEnabled_invoked() {
        RagProperties props = new RagProperties();
        props.getQueryRewriter().setEnabled(false);
        props.getReranker().setEnabled(true);

        QueryRewriter rw = QueryRewriter.noOp();
        Retriever ret = new Retriever(null, null, props) {
            @Override
            public List<ScoredChunk> retrieve(String query, Integer t, Double m,
                                               Set<Long> d, Set<String> s) {
                return List.of(new ScoredChunk(1L, 10L, "x", 0.9, java.util.Map.of()),
                               new ScoredChunk(2L, 10L, "y", 0.7, java.util.Map.of()));
            }
        };
        java.util.concurrent.atomic.AtomicInteger called = new java.util.concurrent.atomic.AtomicInteger();
        Reranker rk = (q, c) -> { called.incrementAndGet(); return c.reversed(); };

        RagPipeline p = new RagPipeline(rw, ret, rk, props);
        List<ScoredChunk> hits = p.search("hello", null, null, null, null);
        assertEquals(1, called.get());
        assertEquals(2L, hits.get(0).chunkId()); // reversed → 0.7 在前
    }

    @Test
    void search_nullQuery_emptyResult() {
        RagProperties props = new RagProperties();
        props.getQueryRewriter().setEnabled(false);
        Retriever stub = new Retriever(null, null, props) {
            @Override
            public List<ScoredChunk> retrieve(String query, Integer t, Double m,
                                               Set<Long> d, Set<String> s) {
                return List.of();
            }
        };
        RagPipeline p = new RagPipeline(QueryRewriter.noOp(), stub, (q, c) -> c, props);
        assertTrue(p.search(null, null, null, null, null).isEmpty());
    }
}
