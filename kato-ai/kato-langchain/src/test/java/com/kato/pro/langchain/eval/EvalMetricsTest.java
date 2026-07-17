package com.kato.pro.langchain.eval;

import com.kato.pro.langchain.domain.knowledge.ScoredChunk;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * EvalMetrics 单元测试：覆盖单条计算 + 聚合 + 边界情况。
 */
class EvalMetricsTest {

    private static ScoredChunk chunk(long chunkId) {
        return new ScoredChunk(chunkId, 1L, "content-" + chunkId, 0.9, Map.of());
    }

    private static GoldenCase golden(String id, List<Long> expected) {
        return GoldenCase.builder()
                .id(id).query("q-" + id)
                .expectedChunkIds(expected)
                .category("test").notes("")
                .build();
    }

    @Test
    void compute_allHitsAtTop1_recallMrrHitAllOne() {
        GoldenCase g = golden("c1", List.of(100L, 200L));
        List<ScoredChunk> candidates = List.of(chunk(100L), chunk(200L), chunk(999L));
        EvalMetrics m = EvalMetrics.compute(g, candidates, 5L);

        assertEquals(1.0, m.getRecallAtK(), 1e-9);
        assertEquals(1.0, m.getMrr(), 1e-9);
        assertEquals(1.0, m.getHitRate(), 1e-9);
        assertEquals(2, m.getHitCount());
        assertEquals(2, m.getExpectedCount());
    }

    @Test
    void compute_partialHit_mrrReflectsFirstHitRank() {
        GoldenCase g = golden("c2", List.of(500L));
        // expected=500 出现在第 3 位 → mrr = 1/3
        List<ScoredChunk> candidates = List.of(chunk(1L), chunk(2L), chunk(500L));
        EvalMetrics m = EvalMetrics.compute(g, candidates, 3L);

        assertEquals(1.0, m.getRecallAtK(), 1e-9);
        assertEquals(1.0 / 3.0, m.getMrr(), 1e-9);
        assertEquals(1.0, m.getHitRate(), 1e-9);
    }

    @Test
    void compute_noHit_recallMrrHitAllZero() {
        GoldenCase g = golden("c3", List.of(700L, 800L));
        List<ScoredChunk> candidates = List.of(chunk(1L), chunk(2L), chunk(3L));
        EvalMetrics m = EvalMetrics.compute(g, candidates, 1L);

        assertEquals(0.0, m.getRecallAtK(), 1e-9);
        assertEquals(0.0, m.getMrr(), 1e-9);
        assertEquals(0.0, m.getHitRate(), 1e-9);
        assertEquals(0, m.getHitCount());
    }

    @Test
    void compute_emptyCandidates_allZero() {
        GoldenCase g = golden("c4", List.of(1L));
        EvalMetrics m = EvalMetrics.compute(g, List.of(), 1L);

        assertEquals(0.0, m.getRecallAtK(), 1e-9);
        assertEquals(0.0, m.getMrr(), 1e-9);
        assertEquals(0.0, m.getHitRate(), 1e-9);
        assertEquals(0, m.getTopK());
    }

    @Test
    void compute_emptyExpected_recallZeroAndNoDivisionByZero() {
        GoldenCase g = golden("c5", new ArrayList<>());
        List<ScoredChunk> candidates = List.of(chunk(1L));
        EvalMetrics m = EvalMetrics.compute(g, candidates, 1L);

        // expected 为空 → recall 应当是 0 而非抛除零
        assertEquals(0.0, m.getRecallAtK(), 1e-9);
        assertEquals(0.0, m.getHitRate(), 1e-9);
        assertEquals(0, m.getExpectedCount());
    }

    @Test
    void aggregate_meansOfAllMetrics() {
        EvalMetrics m1 = new EvalMetrics("a", "q", 1.0, 1.0, 1.0, 10L, 5, 1, 1, List.of(1L), List.of(1L));
        EvalMetrics m2 = new EvalMetrics("b", "q", 0.0, 0.0, 0.0, 20L, 5, 1, 0, List.of(), List.of(2L));

        EvalMetrics.EvalAggregate agg = EvalMetrics.aggregate(List.of(m1, m2));

        assertEquals(2, agg.getTotal());
        assertEquals(0.5, agg.getMeanRecallAtK(), 1e-9);
        assertEquals(0.5, agg.getMeanMrr(), 1e-9);
        assertEquals(0.5, agg.getMeanHitRate(), 1e-9);
        assertEquals(15.0, agg.getMeanLatencyMs(), 1e-9);
    }

    @Test
    void aggregate_emptyList_returnsZeroAggregate() {
        EvalMetrics.EvalAggregate agg = EvalMetrics.aggregate(List.of());
        assertNotNull(agg);
        assertEquals(0, agg.getTotal());
        assertEquals(0.0, agg.getMeanRecallAtK());
        assertEquals(0.0, agg.getMeanMrr());
        assertEquals(0.0, agg.getMeanHitRate());
        assertEquals(0.0, agg.getMeanLatencyMs());
    }

    @Test
    void aggregate_singleEntry_returnsThatEntry() {
        EvalMetrics m = new EvalMetrics("solo", "q", 0.8, 0.5, 1.0, 7L, 5, 2, 1, List.of(1L), List.of(1L));
        EvalMetrics.EvalAggregate agg = EvalMetrics.aggregate(List.of(m));

        assertEquals(1, agg.getTotal());
        assertEquals(0.8, agg.getMeanRecallAtK(), 1e-9);
        assertEquals(0.5, agg.getMeanMrr(), 1e-9);
        assertEquals(1.0, agg.getMeanHitRate(), 1e-9);
        assertTrue(agg.getMeanLatencyMs() == 7.0);
    }
}
