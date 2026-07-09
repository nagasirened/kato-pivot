package com.kato.pro.langchain.domain.rag;

import com.kato.pro.langchain.domain.knowledge.IndexedChunk;
import com.kato.pro.langchain.domain.knowledge.ScoredChunk;

import java.util.List;
import java.util.Set;

/**
 * 向量存储 SPI。
 *
 * 实现：
 *   - v1: InMemoryVectorStore（ConcurrentHashMap）
 *   - v2: MilvusVectorStore / QdrantVectorStore / PgVectorVectorStore
 */
public interface VectorStore {

    /** 加入一条向量索引 */
    void add(IndexedChunk chunk);

    /**
     * 检索：余弦相似度（D3=A）+ 过滤（tenant / docIds / sourceTypes）
     *
     * @param tenantId    租户隔离
     * @param queryVector 查询向量
     * @param topK        返回前 K 个
     * @param minScore    最低分（cosine ∈ [-1, 1]）
     * @param docIds      限定文档集合（null/empty = 不限）
     * @param sourceTypes 限定来源类型（null/empty = 不限）
     */
    List<ScoredChunk> search(Long tenantId, float[] queryVector, int topK, double minScore,
                              Set<Long> docIds, Set<String> sourceTypes);

    /** 按 docId 删除全部 chunks */
    void deleteByDocId(Long tenantId, Long docId);

    /** 当前总 chunk 数（用于监控/测试） */
    long size();

    /** 生成下一个 vector_ref（v1 = InMemory 自增） */
    long nextVectorRef();
}
