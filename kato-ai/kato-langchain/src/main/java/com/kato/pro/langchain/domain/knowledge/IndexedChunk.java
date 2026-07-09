package com.kato.pro.langchain.domain.knowledge;

/**
 * 向量索引条目（同时持有向量 + 业务元数据）。
 *
 * 注意：vector 是 float[] — 写入 VectorStore 时按需序列化。
 */
public record IndexedChunk(Long tenantId, Long chunkId, Long docId, String sourceType,
                            int chunkIndex, String content, float[] vector) {}
