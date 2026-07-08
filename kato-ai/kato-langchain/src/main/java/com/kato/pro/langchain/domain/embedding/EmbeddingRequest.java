package com.kato.pro.langchain.domain.embedding;

import lombok.Builder;
import lombok.Value;

import java.util.List;

/**
 * Embedding 请求。
 *
 * 设计：单条/批量都通过 List&lt;String&gt; 表达，便于实现方内部批处理。
 * inputs 列表长度 = 返回向量的个数。
 */
@Value
@Builder
public class EmbeddingRequest {

    /** 待向量化的文本列表。 */
    List<String> inputs;

    /** 可选：用户/租户标识（用于 audit / 多租户限流）。M2 不强制。 */
    String userId;
}
