package com.kato.pro.langchain.domain.embedding;

import lombok.Builder;
import lombok.Value;

import java.util.List;

/**
 * Embedding 响应。
 *
 * vectors 列表与请求 inputs 一一对应。维度由实现决定（M2 = 512）。
 */
@Value
@Builder
public class EmbeddingResponse {

    /** 向量列表，顺序与请求 inputs 一致。 */
    List<float[]> vectors;

    /** 实际模型名（便于 audit / 切换追踪）。 */
    String model;

    /** 可选：token 消耗。M2 fake impl 返回 null。 */
    Integer tokensConsumed;
}
