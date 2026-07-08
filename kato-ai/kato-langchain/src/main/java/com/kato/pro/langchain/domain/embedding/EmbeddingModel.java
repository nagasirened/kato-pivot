package com.kato.pro.langchain.domain.embedding;

import reactor.core.publisher.Mono;

/**
 * Embedding 模型抽象。
 *
 * 实现：
 *   - v1: InMemoryFakeEmbeddingModel（M2 实现，沙箱可跑）
 *   - v2: BgeSmallZhEmbeddingModel（ONNX Runtime 加载 bge-small-zh-v1.5）
 *   - v2: RemoteHttpEmbeddingModel（HTTP 调用 MiniMax/OpenAI 的 /v1/embeddings）
 */
public interface EmbeddingModel {

    /** 模型名（用于 audit + config 校验）。 */
    String modelName();

    /** 向量维度（用于 RAG 索引建表 / 校验）。 */
    int dimension();

    /** 单条/批量向量化。 */
    Mono<EmbeddingResponse> embed(EmbeddingRequest request);
}
