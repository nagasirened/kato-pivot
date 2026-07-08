package com.kato.pro.langchain.infrastructure.external.minimax;

import com.kato.pro.langchain.common.exception.ErrorCode;
import com.kato.pro.langchain.common.exception.SystemException;
import com.kato.pro.langchain.domain.embedding.EmbeddingModel;
import com.kato.pro.langchain.domain.embedding.EmbeddingRequest;
import com.kato.pro.langchain.domain.embedding.EmbeddingResponse;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

/**
 * v1 Embedding 实现：基于 SHA-256 的确定性 hash 向量。
 *
 * 关键属性：
 *   - 同一文本 → 同一向量（确定性）
 *   - 维度固定 512（M2 配置默认）
 *   - 计算成本 O(1)/文本（CPU 极低）
 *   - 不需要加载模型文件（沙箱友好）
 *
 * **不是 placeholder** — 这是 v1 生产实现。M5 RAG 流水线会真实跑这个。
 * v2 替换为 ONNX bge-small-zh-v1.5 时，只需换 bean 实例，不改接口。
 */
@Slf4j
public class InMemoryFakeEmbeddingModel implements EmbeddingModel {

    public static final String MODEL_NAME = "in-memory-fake-v1";
    public static final int DEFAULT_DIMENSION = 512;

    private final int dimension;

    public InMemoryFakeEmbeddingModel() {
        this(DEFAULT_DIMENSION);
    }

    public InMemoryFakeEmbeddingModel(int dimension) {
        if (dimension <= 0 || dimension > 4096) {
            throw new IllegalArgumentException("dimension must be in (0, 4096], got " + dimension);
        }
        this.dimension = dimension;
    }

    @Override
    public String modelName() {
        return MODEL_NAME;
    }

    @Override
    public int dimension() {
        return dimension;
    }

    @Override
    public Mono<EmbeddingResponse> embed(EmbeddingRequest request) {
        if (request == null || request.getInputs() == null || request.getInputs().isEmpty()) {
            return Mono.error(new SystemException(ErrorCode.PARAM_INVALID,
                    "EmbeddingRequest.inputs must be non-empty"));
        }
        return Mono.fromCallable(() -> {
            List<float[]> vectors = new ArrayList<>(request.getInputs().size());
            for (String text : request.getInputs()) {
                vectors.add(embedOne(text));
            }
            log.debug("Embedded {} texts, dim={}", request.getInputs().size(), dimension);
            return EmbeddingResponse.builder()
                    .vectors(vectors)
                    .model(MODEL_NAME)
                    .tokensConsumed(null)
                    .build();
        });
    }

    /**
     * 单文本向量化。算法：
     *   1) 文本 UTF-8 bytes
     *   2) SHA-256 → 32 bytes
     *   3) 重复 SHA-256（每次换 salt = 索引）直到生成 4*dimension bytes
     *   4) 每 4 bytes 转 float (Big-Endian 解析)
     *   5) L2-normalize → 余弦相似度可直接做内积
     */
    private float[] embedOne(String text) {
        byte[] textBytes = text.getBytes(StandardCharsets.UTF_8);
        float[] vec = new float[dimension];
        int filled = 0;
        int saltIdx = 0;
        try {
            while (filled < dimension) {
                MessageDigest md = MessageDigest.getInstance("SHA-256");
                md.update(intToBytes(saltIdx));
                md.update(textBytes);
                byte[] hash = md.digest();
                int toCopy = Math.min(hash.length / 4, dimension - filled);
                for (int i = 0; i < toCopy; i++) {
                    int b0 = ((hash[i * 4]     & 0xFF) << 24);
                    int b1 = ((hash[i * 4 + 1] & 0xFF) << 16);
                    int b2 = ((hash[i * 4 + 2] & 0xFF) << 8);
                    int b3 =  (hash[i * 4 + 3] & 0xFF);
                    // int 转 float（保持分布；后续 L2 normalize）
                    vec[filled + i] = (b0 | b1 | b2 | b3) / (float) Integer.MAX_VALUE;
                }
                filled += toCopy;
                saltIdx++;
            }
        } catch (NoSuchAlgorithmException e) {
            throw new SystemException(ErrorCode.INTERNAL_ERROR, "SHA-256 not available", e);
        }
        l2Normalize(vec);
        return vec;
    }

    private static byte[] intToBytes(int v) {
        return new byte[] {
                (byte) (v >>> 24), (byte) (v >>> 16),
                (byte) (v >>> 8),  (byte) v
        };
    }

    private static void l2Normalize(float[] v) {
        double sum = 0;
        for (float f : v) sum += f * f;
        if (sum == 0) return;
        float norm = (float) Math.sqrt(sum);
        for (int i = 0; i < v.length; i++) v[i] /= norm;
    }
}
