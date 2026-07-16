package com.kato.pro.langchain.api.prompt.dto;

import com.kato.pro.langchain.domain.prompt.PromptTemplate;
import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;
import io.swagger.v3.oas.annotations.media.Schema;


@Value
@Builder
@Schema(description = "Prompt 模板视图")
public class PromptTemplateVO {
    Long id;
    Long tenantId;
    String templateKey;
    String content;
    Integer version;
    String description;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;

    public static PromptTemplateVO from(PromptTemplate t) {
        if (t == null) return null;
        return PromptTemplateVO.builder()
                .id(t.getId())
                .tenantId(t.getTenantId())
                .templateKey(t.getTemplateKey())
                .content(t.getContent())
                .version(t.getVersion())
                .description(t.getDescription())
                .createdAt(t.getCreateTime())
                .updatedAt(t.getUpdateTime())
                .build();
    }
}
