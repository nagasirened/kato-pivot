package com.kato.pro.langchain.domain.knowledge;

import java.util.Collections;
import java.util.Map;

/**
 * 检索结果条目。
 *
 * metadata 用于前端展示（docTitle / chunkIndex / sourceType），不直接进 prompt。
 */
public record ScoredChunk(Long chunkId, Long docId, String content, double score,
                           Map<String, Object> metadata) {

    public ScoredChunk {
        metadata = metadata == null ? Map.of() : Collections.unmodifiableMap(metadata);
    }
}
