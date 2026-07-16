package com.kato.pro.langchain.api.knowledge.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * RAG 检索请求体。M8 ChatEngine 也会用同款结构组装。
 */
@Data
@Schema(description = "RAG 检索请求体")
public class SearchRequest {

    @NotBlank(message = "query 不能为空")
    @Size(max = 500, message = "query 长度不能超过 500")
    @Schema(description = "检索 query", example = "退货流程是什么？",
            requiredMode = Schema.RequiredMode.REQUIRED,
            minLength = 1, maxLength = 500)
    private String query;

    @Schema(description = "返回 topK 个结果", example = "5",
            minimum = "1", maximum = "50",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private Integer topK;

    @Schema(description = "最小相似度阈值", example = "0.6",
            minimum = "0", maximum = "1",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private Double minScore;

    /** 限定文档 ID 列表（null = 不限） */
    @Schema(description = "限定文档 ID 列表（null = 不限）",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private List<Long> docIds;

    /** 限定 source types（null = 不限）：UPLOAD / SYNC_PRODUCT / ... */
    @Schema(description = "限定 source types（null = 不限）：UPLOAD / SYNC_PRODUCT / ...",
            example = "[\"UPLOAD\"]",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private List<String> sourceTypes;
}
