package com.kato.pro.langchain.api.knowledge.dto;

import com.kato.pro.langchain.domain.knowledge.ScoredChunk;
import lombok.Builder;
import lombok.Value;

import java.util.List;
import java.util.Map;
import io.swagger.v3.oas.annotations.media.Schema;


@Value
@Builder
@Schema(description = "RAG 检索结果视图")
public class SearchResultVO {
    String query;
    String rewrittenQuery;
    int hitCount;
    List<Hit> hits;

    @Value
    @Builder
    public static class Hit {
        Long chunkId;
        Long docId;
        String content;
        double score;
        Map<String, Object> metadata;
    }

    public static SearchResultVO build(String originalQuery, String rewrittenQuery, List<ScoredChunk> chunks) {
        List<Hit> hits = chunks.stream()
                .map(c -> Hit.builder()
                        .chunkId(c.chunkId())
                        .docId(c.docId())
                        .content(c.content())
                        .score(c.score())
                        .metadata(c.metadata())
                        .build())
                .toList();
        return SearchResultVO.builder()
                .query(originalQuery)
                .rewrittenQuery(rewrittenQuery)
                .hitCount(hits.size())
                .hits(hits)
                .build();
    }
}
