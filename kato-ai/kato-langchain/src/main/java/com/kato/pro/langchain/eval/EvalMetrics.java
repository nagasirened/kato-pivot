package com.kato.pro.langchain.eval;

import com.kato.pro.langchain.domain.knowledge.ScoredChunk;
import lombok.Value;

import java.util.List;

/**
 * 单条 Golden Case 的检索质量指标。
 *
 * 指标语义：
 *   - recallAtK   = |topK 命中 expected ∩ expected| / |expected|  ∈ [0, 1]
 *   - mrr         = 1 / firstHitRank（如 1st 命中排第 3 位 → 1/3 = 0.333）；未命中 → 0
 *   - hitRate     = 1 if topK 至少一条命中 expected else 0  ∈ {0, 1}
 *   - latencyMs   = 单次 pipeline.search 耗时（毫秒）
 *   - hitChunkIds  实际命中的 expected chunkId（用于失败时报告）
 *
 * 排序口径：candidates 已经是 Retriever/Reranker 排序后的 topK 列表。
 */
@Value
public class EvalMetrics {

    String caseId;
    String query;
    double recallAtK;
    double mrr;
    double hitRate;
    long latencyMs;
    int topK;
    int expectedCount;
    int hitCount;
    List<Long> hitChunkIds;
    List<Long> retrievedChunkIds;

    /**
     * 计算单条 case 的指标。
     *
     * @param case       golden case
     * @param candidates pipeline.search 返回的 topK
     * @param latencyMs  检索耗时
     */
    public static EvalMetrics compute(GoldenCase golden, List<ScoredChunk> candidates, long latencyMs) {
        if (golden == null) throw new IllegalArgumentException("golden must not be null");
        List<Long> expected = golden.getExpectedChunkIds() == null ? List.of() : golden.getExpectedChunkIds();
        int topK = candidates == null ? 0 : candidates.size();

        List<Long> retrievedIds = candidates == null
                ? List.of()
                : candidates.stream().map(ScoredChunk::chunkId).toList();

        // 命中：retrieved 中存在 expected 任一条
        int hitCount = 0;
        for (Long exp : expected) {
            if (retrievedIds.contains(exp)) hitCount++;
        }
        double recall = expected.isEmpty() ? 0.0 : (double) hitCount / expected.size();
        double hit = hitCount > 0 ? 1.0 : 0.0;

        // MRR：第一个命中的 expected 在 retrieved 中的位置（1-based）
        double mrr = 0.0;
        if (hitCount > 0) {
            for (int i = 0; i < retrievedIds.size(); i++) {
                if (expected.contains(retrievedIds.get(i))) {
                    mrr = 1.0 / (i + 1);
                    break;
                }
            }
        }

        // 命中的 expected 子集
        List<Long> hits = expected.stream().filter(retrievedIds::contains).toList();

        return new EvalMetrics(
                golden.getId(),
                golden.getQuery(),
                recall,
                mrr,
                hit,
                latencyMs,
                topK,
                expected.size(),
                hitCount,
                hits,
                retrievedIds
        );
    }

    /**
     * 聚合多条 case 的指标。返回 4 个核心指标的均值 + 总数。
     */
    public static EvalAggregate aggregate(List<EvalMetrics> metrics) {
        if (metrics == null || metrics.isEmpty()) {
            return new EvalAggregate(0, 0.0, 0.0, 0.0, 0.0);
        }
        int n = metrics.size();
        double sumRecall = 0, sumMrr = 0, sumHit = 0, sumLatency = 0;
        for (EvalMetrics m : metrics) {
            sumRecall += m.getRecallAtK();
            sumMrr += m.getMrr();
            sumHit += m.getHitRate();
            sumLatency += m.getLatencyMs();
        }
        return new EvalAggregate(
                n,
                sumRecall / n,
                sumMrr / n,
                sumHit / n,
                sumLatency / n
        );
    }

    /**
     * 聚合结果（用于报告 & gate）。
     */
    @Value
    public static class EvalAggregate {
        int total;
        double meanRecallAtK;
        double meanMrr;
        double meanHitRate;
        double meanLatencyMs;
    }
}
