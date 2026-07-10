package com.kato.pro.langchain.common.metrics;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * MetricRegistry 单元测试（M11）。
 */
class MetricRegistryTest {

    @BeforeEach
    void setUp() { MetricRegistry.reset(); }

    @Test
    void counter_incrementsAndCounts() {
        MetricRegistry.Counter c = MetricRegistry.counter("test.counter");
        assertEquals(0, c.count());
        c.inc();
        c.inc(5);
        assertEquals(6, c.count());
    }

    @Test
    void counter_withTag_keysAreDistinct() {
        MetricRegistry.Counter a = MetricRegistry.counter("hits", "kind=input");
        MetricRegistry.Counter b = MetricRegistry.counter("hits", "kind=output");
        a.inc(); a.inc(); b.inc();
        assertEquals(2, a.count());
        assertEquals(1, b.count());
    }

    @Test
    void recordValue_storesGauge() {
        MetricRegistry.recordValue("gauge.test", 12.5);
        MetricRegistry.recordValue("gauge.test", 13.0);
        List<MetricRegistry.MetricSnapshot> snap = MetricRegistry.snapshot();
        MetricRegistry.MetricSnapshot s = snap.stream()
                .filter(x -> "gauge.test".equals(x.name())).findFirst().orElseThrow();
        assertEquals(13.0, s.value(), 0.001);
        assertEquals("gauge", s.type());
    }

    @Test
    void snapshot_includesCountersAndGauges_sorted() {
        MetricRegistry.counter("zeta").inc();
        MetricRegistry.counter("alpha").inc();
        MetricRegistry.recordValue("middle.gauge", 1.0);
        List<MetricRegistry.MetricSnapshot> snap = MetricRegistry.snapshot();
        // 至少 3 条
        assertTrue(snap.size() >= 3);
        // 排序：name asc
        for (int i = 0; i < snap.size() - 1; i++) {
            assertTrue(snap.get(i).name().compareTo(snap.get(i+1).name()) <= 0);
        }
    }

    @Test
    void counter_isThreadSafe() throws Exception {
        MetricRegistry.Counter c = MetricRegistry.counter("concurrent.test");
        int threads = 8;
        int incsPerThread = 1000;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);
        for (int i = 0; i < threads; i++) {
            pool.submit(() -> {
                try {
                    start.await();
                    for (int j = 0; j < incsPerThread; j++) c.inc();
                } catch (InterruptedException ignored) {
                } finally {
                    done.countDown();
                }
            });
        }
        start.countDown();
        assertTrue(done.await(5, TimeUnit.SECONDS));
        pool.shutdown();
        assertEquals((long) threads * incsPerThread, c.count());
    }
}
