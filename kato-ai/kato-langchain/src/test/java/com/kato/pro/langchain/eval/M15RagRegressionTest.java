package com.kato.pro.langchain.eval;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * M15 RAG 回归门禁。
 *
 * 守住阈值（CI 阻断线）：
 *   - Recall@K ≥ 0.7
 *   - Hit Rate ≥ 0.8
 *   - MRR    ≥ 0.5
 *
 * 任一不满足 → fail 并打印每条 case 的指标。
 *
 * 手动 wire EvalRunner（不依赖 Spring 容器），沙箱 JDK21 友好。
 */
class M15RagRegressionTest {

    private static final double RECALL_THRESHOLD = 0.7;
    private static final double HIT_RATE_THRESHOLD = 0.8;
    private static final double MRR_THRESHOLD = 0.5;

    @Test
    void runAllGoldenCases_meetsThresholds() {
        List<GoldenCase> golden = GoldenSetLoader.loadDefault();
        assertTrue(golden != null && !golden.isEmpty(), "Golden set must not be empty");

        EvalRunner runner = new EvalRunner(1L, 5, 0.0);
        EvalRunner.EvalReport report = runner.run(golden);
        EvalMetrics.EvalAggregate agg = report.getAggregate();

        // 打印每条 case（debug 友好）
        StringBuilder detail = new StringBuilder();
        detail.append(String.format(
                "RAG eval aggregate: cases=%d recall@K=%.3f mrr=%.3f hitRate=%.3f latency=%.1fms%n",
                agg.getTotal(), agg.getMeanRecallAtK(), agg.getMeanMrr(),
                agg.getMeanHitRate(), agg.getMeanLatencyMs()));
        for (EvalMetrics m : report.getPerCase()) {
            detail.append(String.format("  [%s] hit=%d/%d recall=%.2f mrr=%.2f latency=%dms query=%s%n",
                    m.getCaseId(), m.getHitCount(), m.getExpectedCount(),
                    m.getRecallAtK(), m.getMrr(), m.getLatencyMs(), m.getQuery()));
        }

        // 失败时打印整张表（不丢上下文）
        if (agg.getMeanRecallAtK() < RECALL_THRESHOLD
                || agg.getMeanHitRate() < HIT_RATE_THRESHOLD
                || agg.getMeanMrr() < MRR_THRESHOLD) {
            fail("RAG eval below threshold (recall@" + RECALL_THRESHOLD
                    + ", hitRate>=" + HIT_RATE_THRESHOLD
                    + ", mrr>=" + MRR_THRESHOLD + ")\n" + detail);
        }
    }
}
