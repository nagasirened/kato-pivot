package com.kato.pro.langchain.api.knowledge.dto;

import com.kato.pro.langchain.domain.knowledge.KnowledgeDoc;
import com.kato.pro.langchain.domain.knowledge.KnowledgeDocStatus;
import com.kato.pro.langchain.domain.knowledge.SourceType;
import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

@Value
@Builder
public class KnowledgeDocVO {
    Long id;
    String title;
    String fileName;
    String contentType;
    SourceType sourceType;
    String sourceId;
    KnowledgeDocStatus status;
    String errorMessage;
    Integer chunkCount;
    Integer charCount;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;

    public static KnowledgeDocVO from(KnowledgeDoc d) {
        if (d == null) return null;
        return KnowledgeDocVO.builder()
                .id(d.getId())
                .title(d.getTitle())
                .fileName(d.getFileName())
                .contentType(d.getContentType())
                .sourceType(d.getSourceType())
                .sourceId(d.getSourceId())
                .status(d.getStatus())
                .errorMessage(d.getErrorMessage())
                .chunkCount(d.getChunkCount())
                .charCount(d.getCharCount())
                .createdAt(d.getCreateTime())
                .updatedAt(d.getUpdateTime())
                .build();
    }
}
