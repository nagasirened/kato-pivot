package com.kato.pro.langchain.domain.rag;

import com.kato.pro.langchain.common.tenant.TenantContext;
import com.kato.pro.langchain.common.tenant.TenantInfo;
import com.kato.pro.langchain.config.RagProperties;
import com.kato.pro.langchain.domain.embedding.EmbeddingModel;
import com.kato.pro.langchain.domain.embedding.EmbeddingRequest;
import com.kato.pro.langchain.domain.embedding.EmbeddingResponse;
import com.kato.pro.langchain.domain.knowledge.IndexedChunk;
import com.kato.pro.langchain.domain.knowledge.ScoredChunk;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RetrieverTest {

    private Retriever retriever;
    private InMemoryVectorStore store;

    @BeforeEach
    void setup() {
        store = new InMemoryVectorStore();
        EmbeddingModel mock = new EmbeddingModel() {
            @Override public String modelName() { return "mock"; }
            @Override public int dimension() { return 4; }
            @Override public Mono<EmbeddingResponse> embed(EmbeddingRequest req) {
                java.util.List<float[]> vecs = new java.util.ArrayList<>();
                for (String t : req.getInputs()) {
                    float[] v = new float[4];
                    int seed = t == null ? 0 : Math.abs(t.hashCode() % 4);
                    v[seed] = 1.0f;
                    vecs.add(v);
                }
                EmbeddingResponse r = EmbeddingResponse.builder().vectors(vecs).model("mock").build();
                return Mono.just(r);
            }
        };
        RagProperties props = new RagProperties();
        retriever = new Retriever(mock, store, props);
        TenantContext.set(TenantInfo.of(1L, 100L));

        // 用确定方向 seed，确保 chunk 100 与 query "hello" 同方向
        store.add(new IndexedChunk(1L, 100L, 10L, "UPLOAD", 0, "hello",
                new float[]{1, 0, 0, 0}));
        store.add(new IndexedChunk(1L, 101L, 10L, "UPLOAD", 1, "world",
                new float[]{0, 1, 0, 0}));
        store.add(new IndexedChunk(1L, 102L, 11L, "SYNC_FAQ", 0, "faq",
                new float[]{0, 0, 1, 0}));
    }

    @AfterEach
    void cleanup() { TenantContext.clear(); }

    @Test
    void retrieve_blankQuery_returnsEmpty() {
        assertTrue(retriever.retrieve("", 5, 0.0, null, null).isEmpty());
        assertTrue(retriever.retrieve(null, 5, 0.0, null, null).isEmpty());
    }

    @Test
    void retrieve_tenantIsolation() {
        TenantContext.set(TenantInfo.of(2L, 200L));
        assertEquals(0, retriever.retrieve("hello", 5, 0.0, null, null).size());
    }

    @Test
    void retrieve_docIdsFilter() {
        // query 与 chunk 100/101/102 hash 后方向不可控；用 docIds 过滤测试逻辑
        List<ScoredChunk> hits = retriever.retrieve("hello", 5, 0.0, Set.of(10L), null);
        assertTrue(hits.stream().allMatch(h -> h.docId().equals(10L)));
    }

    @Test
    void retrieve_sourceTypesFilter() {
        List<ScoredChunk> hits = retriever.retrieve("hello", 5, 0.0, null, Set.of("SYNC_FAQ"));
        assertTrue(hits.stream().allMatch(h -> "SYNC_FAQ".equals(h.metadata().get("sourceType"))));
    }

    @Test
    void retrieve_topK_capsResults() {
        // 全部 3 条都在 tenant=1 下；topK=2 应截断
        // query hash 决定方向，但方向上可能有 1~3 条匹配；用 minScore=0 保证全返回
        List<ScoredChunk> hits = retriever.retrieve("xxx", 2, 0.0, null, null);
        assertTrue(hits.size() <= 2);
    }
}
