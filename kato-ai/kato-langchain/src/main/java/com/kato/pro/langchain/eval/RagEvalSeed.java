package com.kato.pro.langchain.eval;

import com.kato.pro.langchain.domain.embedding.EmbeddingModel;
import com.kato.pro.langchain.domain.embedding.EmbeddingRequest;
import com.kato.pro.langchain.domain.embedding.EmbeddingResponse;
import com.kato.pro.langchain.domain.knowledge.IndexedChunk;
import com.kato.pro.langchain.domain.knowledge.SourceType;
import com.kato.pro.langchain.domain.rag.InMemoryVectorStore;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

/**
 * 评测用 seed data — 灌入预设 IndexedChunk。
 *
 * 关键设计（保证 SHA-256 hash embedding 评测生效）：
 *   - 业务 chunk 内容 = query 原文（deterministic 下 cosine=1）
 *   - 同一 expectedChunkIds 多 chunk 的 case 用变体（query + 序号）保证 chunkId 唯一映射
 *   - 噪声 chunk 文本 = 完全不同的领域词，hash 与 query 完全不同 → cosine ≈ 0
 *
 * chunkId 分布：
 *   - 1001+        业务 chunk（与 golden-set.json expectedChunkIds 对齐）
 *   - 5000+        噪声 chunk
 */
@Slf4j
public final class RagEvalSeed {

    public static final long NOISE_BASE_ID = 5000L;
    public static final int NOISE_COUNT = 160;

    private RagEvalSeed() {
    }

    public static void seed(InMemoryVectorStore vectorStore, EmbeddingModel embedding, long tenantId) {
        List<GoldenCase> golden = GoldenSetLoader.loadDefault();
        seed(vectorStore, embedding, tenantId, golden);
    }

    public static void seed(InMemoryVectorStore vectorStore, EmbeddingModel embedding, long tenantId,
                            List<GoldenCase> golden) {
        if (golden == null || golden.isEmpty()) {
            throw new IllegalArgumentException("golden set must be non-empty");
        }

        List<IndexedChunk> toAdd = new ArrayList<>();
        List<String> texts = new ArrayList<>();

        // 1) 业务 chunk：内容 = query 原文（hash cosine=1）
        for (GoldenCase gc : golden) {
            List<Long> expected = gc.getExpectedChunkIds();
            if (expected == null || expected.isEmpty()) continue;
            // 单 expected：内容 = query 原文 → cosine=1
            // 多 expected：第 1 个 = query 原文，其余 = query + 序号变体（仍保留 query 主体）
            for (int i = 0; i < expected.size(); i++) {
                Long chunkId = expected.get(i);
                String content = i == 0 ? gc.getQuery() : gc.getQuery() + "（补充 " + (i + 1) + "）";
                texts.add(content);
                toAdd.add(new IndexedChunk(
                        tenantId,
                        chunkId,
                        docIdForCategory(gc.getCategory()),
                        SourceType.UPLOAD.code(),
                        i,
                        content,
                        new float[embedding.dimension()]
                ));
            }
        }

        // 2) 噪声 chunk
        List<String> noise = noiseTexts();
        for (int i = 0; i < noise.size(); i++) {
            String content = noise.get(i);
            texts.add(content);
            toAdd.add(new IndexedChunk(
                    tenantId,
                    NOISE_BASE_ID + i,
                    9999L,
                    SourceType.UPLOAD.code(),
                    i,
                    content,
                    new float[embedding.dimension()]
            ));
        }

        // 3) 批量向量化
        EmbeddingRequest req = EmbeddingRequest.builder().inputs(texts).build();
        EmbeddingResponse resp = embedding.embed(req).block();
        List<float[]> vectors = resp.getVectors();

        if (vectors.size() != toAdd.size()) {
            throw new IllegalStateException("Embedding count mismatch: texts=" + texts.size()
                    + " vectors=" + vectors.size());
        }

        // 4) 回填向量并写入 store
        for (int i = 0; i < toAdd.size(); i++) {
            IndexedChunk c = toAdd.get(i);
            vectorStore.add(new IndexedChunk(
                    c.tenantId(), c.chunkId(), c.docId(), c.sourceType(),
                    c.chunkIndex(), c.content(), vectors.get(i)
            ));
        }

        log.info("RagEvalSeed loaded: goldenChunks={} noiseChunks={} total={}",
                toAdd.size() - noise.size(), noise.size(), toAdd.size());
    }

    private static long docIdForCategory(String category) {
        if (category == null) return 1L;
        return switch (category) {
            case "退货" -> 100L;
            case "退款" -> 101L;
            case "物流" -> 102L;
            case "订单" -> 103L;
            case "账户" -> 104L;
            case "支付" -> 105L;
            case "优惠" -> 106L;
            case "发票" -> 107L;
            case "FAQ" -> 108L;
            default -> 1L;
        };
    }

    private static List<String> noiseTexts() {
        List<String> seeds = new ArrayList<>(NOISE_COUNT);
        String[] domains = {"宠物", "餐饮", "教育", "旅游", "健身", "美妆", "汽车", "房产",
                "数码", "母婴", "家居", "摄影", "音乐", "游戏", "体育", "新闻", "天气", "股票", "医疗", "法律"};
        for (int i = 0; i < NOISE_COUNT; i++) {
            String domain = domains[i % domains.length];
            seeds.add(domain + "主题内容 #" + i + "：与电商客服无关的 " + domain
                    + " 知识片段，用于稀释检索空间、避免 topK 退化。");
        }
        return seeds;
    }
}
