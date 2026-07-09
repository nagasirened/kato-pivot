package com.kato.pro.langchain.api.knowledge.dto;

import lombok.Data;

import java.util.List;

/**
 * RAG 检索请求体。M8 ChatEngine 也会用同款结构组装。
 */
@Data
public class SearchRequest {
    private String query;
    private Integer topK;
    private Double minScore;
    /** 限定文档 ID 列表（null = 不限） */
    private List<Long> docIds;
    /** 限定 source types（null = 不限）：UPLOAD / SYNC_PRODUCT / ... */
    private List<String> sourceTypes;
}
