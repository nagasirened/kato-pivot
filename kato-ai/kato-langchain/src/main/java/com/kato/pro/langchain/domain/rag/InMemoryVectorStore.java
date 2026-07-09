package com.kato.pro.langchain.domain.rag;

import com.kato.pro.langchain.domain.knowledge.IndexedChunk;
import com.kato.pro.langchain.domain.knowledge.ScoredChunk;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * v1 InMemory 向量存储：
 *   - ConcurrentHashMap<Long tenantId, ConcurrentHashMap<Long chunkId, IndexedChunk>>
 *   - 余弦相似度（D3=A）
 *   - vector_ref 自增（nextVectorRef()）
 *
 * 性能提示：v1 不建向量索引，O(N) 扫描；n < 10000 时够用。v2 换 Milvus 等。
 */
@Component
public class InMemoryVectorStore implements VectorStore {

    private final Map<Long, Map<Long, IndexedChunk>> byTenant = new ConcurrentHashMap<>();
    private final AtomicLong vectorRefSeq = new AtomicLong(0);

    @Override
    public void add(IndexedChunk chunk) {
        if (chunk == null) throw new IllegalArgumentException("chunk must not be null");
        byTenant.computeIfAbsent(chunk.tenantId(), k -> new ConcurrentHashMap<>())
                .put(chunk.chunkId(), chunk);
    }

    @Override
    public List<ScoredChunk> search(Long tenantId, float[] queryVector, int topK, double minScore,
                                     Set<Long> docIds, Set<String> sourceTypes) {
        if (queryVector == null || queryVector.length == 0) return List.of();
        Map<Long, IndexedChunk> map = byTenant.get(tenantId);
        if (map == null || map.isEmpty()) return List.of();

        List<ScoredChunk> hits = new ArrayList<>();
        for (IndexedChunk c : map.values()) {
            if (docIds != null && !docIds.isEmpty() && !docIds.contains(c.docId())) continue;
            if (sourceTypes != null && !sourceTypes.isEmpty()
                    && (c.sourceType() == null || !sourceTypes.contains(c.sourceType()))) continue;
            double score = cosine(queryVector, c.vector());
            if (score >= minScore) {
                hits.add(new ScoredChunk(c.chunkId(), c.docId(), c.content(), score,
                        Map.of("chunkIndex", c.chunkIndex(), "sourceType",
                                c.sourceType() == null ? "" : c.sourceType())));
            }
        }
        hits.sort(Comparator.comparingDouble(ScoredChunk::score).reversed());
        if (hits.size() > topK) return hits.subList(0, topK);
        return hits;
    }

    @Override
    public void deleteByDocId(Long tenantId, Long docId) {
        Map<Long, IndexedChunk> map = byTenant.get(tenantId);
        if (map != null) map.entrySet().removeIf(e -> e.getValue().docId().equals(docId));
    }

    @Override
    public long size() {
        long total = 0;
        for (Map<Long, IndexedChunk> m : byTenant.values()) total += m.size();
        return total;
    }

    @Override
    public long nextVectorRef() {
        return vectorRefSeq.incrementAndGet();
    }

    static double cosine(float[] a, float[] b) {
        if (a == null || b == null || a.length != b.length) return 0.0;
        double dot = 0, na = 0, nb = 0;
        for (int i = 0; i < a.length; i++) {
            dot += (double) a[i] * b[i];
            na += (double) a[i] * a[i];
            nb += (double) b[i] * b[i];
        }
        if (na == 0 || nb == 0) return 0.0;
        return dot / (Math.sqrt(na) * Math.sqrt(nb));
    }
}
