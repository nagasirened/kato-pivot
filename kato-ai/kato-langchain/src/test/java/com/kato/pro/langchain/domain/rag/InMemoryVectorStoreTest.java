package com.kato.pro.langchain.domain.rag;

import com.kato.pro.langchain.domain.knowledge.IndexedChunk;
import com.kato.pro.langchain.domain.knowledge.ScoredChunk;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InMemoryVectorStoreTest {

    private static float[] vec(int seed, int dim) {
        float[] v = new float[dim];
        v[seed % dim] = 1.0f;
        return v;
    }

    @Test
    void addAndSearch_returnsRanked() {
        InMemoryVectorStore s = new InMemoryVectorStore();
        s.add(new IndexedChunk(1L, 100L, 10L, "UPLOAD", 0, "doc1", vec(1, 8)));
        s.add(new IndexedChunk(1L, 101L, 10L, "UPLOAD", 1, "doc2", vec(2, 8)));
        s.add(new IndexedChunk(1L, 102L, 11L, "UPLOAD", 0, "doc3", vec(3, 8)));

        List<ScoredChunk> hits = s.search(1L, vec(2, 8), 5, 0.0, null, null);
        assertEquals(3, hits.size());
        assertEquals(101L, hits.get(0).chunkId());
    }

    @Test
    void search_tenantIsolation() {
        InMemoryVectorStore s = new InMemoryVectorStore();
        s.add(new IndexedChunk(1L, 100L, 10L, "UPLOAD", 0, "a", vec(1, 4)));
        s.add(new IndexedChunk(2L, 200L, 20L, "UPLOAD", 0, "b", vec(1, 4)));

        assertEquals(1, s.search(1L, vec(1, 4), 5, 0.0, null, null).size());
        assertEquals(1, s.search(2L, vec(1, 4), 5, 0.0, null, null).size());
    }

    @Test
    void search_minScoreFilter() {
        InMemoryVectorStore s = new InMemoryVectorStore();
        s.add(new IndexedChunk(1L, 100L, 10L, "UPLOAD", 0, "x", vec(1, 4)));
        s.add(new IndexedChunk(1L, 101L, 10L, "UPLOAD", 0, "y", vec(2, 4)));

        List<ScoredChunk> hits = s.search(1L, vec(1, 4), 5, 0.9, null, null);
        assertEquals(1, hits.size());
        assertEquals(100L, hits.get(0).chunkId());
    }

    @Test
    void search_docIdsFilter() {
        InMemoryVectorStore s = new InMemoryVectorStore();
        s.add(new IndexedChunk(1L, 100L, 10L, "UPLOAD", 0, "a", vec(1, 4)));
        s.add(new IndexedChunk(1L, 101L, 11L, "UPLOAD", 0, "b", vec(1, 4)));

        List<ScoredChunk> hits = s.search(1L, vec(1, 4), 5, 0.0, Set.of(10L), null);
        assertEquals(1, hits.size());
        assertEquals(100L, hits.get(0).chunkId());
    }

    @Test
    void search_sourceTypesFilter() {
        InMemoryVectorStore s = new InMemoryVectorStore();
        s.add(new IndexedChunk(1L, 100L, 10L, "UPLOAD", 0, "a", vec(1, 4)));
        s.add(new IndexedChunk(1L, 101L, 11L, "SYNC_FAQ", 0, "b", vec(1, 4)));

        List<ScoredChunk> hits = s.search(1L, vec(1, 4), 5, 0.0, null, Set.of("SYNC_FAQ"));
        assertEquals(1, hits.size());
        assertEquals(101L, hits.get(0).chunkId());
    }

    @Test
    void deleteByDocId_removes() {
        InMemoryVectorStore s = new InMemoryVectorStore();
        s.add(new IndexedChunk(1L, 100L, 10L, "UPLOAD", 0, "a", vec(1, 4)));
        s.add(new IndexedChunk(1L, 101L, 10L, "UPLOAD", 1, "b", vec(2, 4)));

        s.deleteByDocId(1L, 10L);
        assertEquals(0, s.search(1L, vec(1, 4), 5, 0.0, null, null).size());
    }

    @Test
    void nextVectorRef_increments() {
        InMemoryVectorStore s = new InMemoryVectorStore();
        long a = s.nextVectorRef();
        long b = s.nextVectorRef();
        assertNotEquals(a, b);
    }

    @Test
    void cosine_basic() {
        assertEquals(1.0, InMemoryVectorStore.cosine(new float[]{1, 0}, new float[]{1, 0}), 1e-6);
        assertEquals(0.0, InMemoryVectorStore.cosine(new float[]{1, 0}, new float[]{0, 1}), 1e-6);
        assertEquals(0.0, InMemoryVectorStore.cosine(new float[]{1, 0}, new float[]{1, 0, 0}), 1e-6);
        assertEquals(0.0, InMemoryVectorStore.cosine(new float[]{0, 0}, new float[]{1, 1}), 1e-6);
    }

    @Test
    void size_sumsAcrossTenants() {
        InMemoryVectorStore s = new InMemoryVectorStore();
        s.add(new IndexedChunk(1L, 100L, 10L, "UPLOAD", 0, "a", vec(1, 4)));
        s.add(new IndexedChunk(2L, 200L, 20L, "UPLOAD", 0, "b", vec(1, 4)));
        assertEquals(2L, s.size());
    }

    @Test
    void search_nullQuery_returnsEmpty() {
        InMemoryVectorStore s = new InMemoryVectorStore();
        assertTrue(s.search(1L, null, 5, 0.0, null, null).isEmpty());
        assertTrue(s.search(1L, new float[0], 5, 0.0, null, null).isEmpty());
    }
}
