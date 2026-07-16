package com.kato.pro.metrics;

import io.prometheus.client.CollectorRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * PrometheusMetricsRegistry 单元测试（spec §6 M12）。
 *
 * 覆盖 counter inc / snapshot / reset / tags / namespace 前缀。
 */
class PrometheusMetricsRegistryTest {

    private CollectorRegistry collectorRegistry;
    private PrometheusMetricsRegistry reg;

    @BeforeEach
    void setUp() {
        collectorRegistry = new CollectorRegistry();
        reg = new PrometheusMetricsRegistry(collectorRegistry, "kato");
    }

    @Test
    void counter_noTags_prometheusNameHasNamespacePrefix() {
        MetricsRegistry.CounterRef c = reg.counter("chat.requests");
        c.inc();
        c.inc(2);

        assertEquals(3, c.count());
        // snapshot 含原始 key "kato_chat.requests"
        var snap = reg.snapshot();
        assertTrue(snap.stream().anyMatch(s -> s.name().equals("kato_chat_requests")
                        && s.type().equals("counter")
                        && s.value() == 3.0),
                "expected counter snapshot for kato_chat.requests = 3, got: " + snap);
    }

    @Test
    void counter_withTags_createsTaggedPrometheusCounter() {
        MetricsRegistry.CounterRef c = reg.counter("http.requests", "method=GET,status=200");
        c.inc(5);

        assertEquals(5L, c.count());
        var snap = reg.snapshot();
        // 简单实现把 tag values 拼到 metric name 后缀
        assertTrue(snap.stream().anyMatch(s -> s.name().contains("http_requests")
                        && s.name().contains("GET") && s.name().contains("200")),
                "expected snapshot entry tagged with GET and 200, got: " + snap);
    }

    @Test
    void counter_sameName_returnsSameInstance() {
        MetricsRegistry.CounterRef a = reg.counter("shared.counter");
        MetricsRegistry.CounterRef b = reg.counter("shared.counter");

        a.inc();
        // 二次拿到的是同一个 CounterRef，b.inc 应在 a 之上累加
        b.inc();
        assertEquals(2, a.count());
        assertEquals(2, b.count());
    }

    @Test
    void recordValue_storesGaugeInSnapshot() {
        reg.recordValue("queue.depth", 42.0);
        reg.recordValue("queue.depth", 7.0);

        var snap = reg.snapshot();
        var gauge = snap.stream().filter(s -> s.name().equals("kato_queue.depth")).findFirst();
        assertTrue(gauge.isPresent(), "expected gauge kato_queue.depth in snapshot, got: " + snap);
        assertEquals("gauge", gauge.get().type());
        // recordValue 覆盖语义
        assertEquals(7.0, gauge.get().value());
    }

    @Test
    void reset_clearsAllCountersAndGauges() {
        reg.counter("c1").inc();
        reg.counter("c2", "k=v").inc();
        reg.recordValue("g1", 1.0);

        assertFalse(reg.snapshot().isEmpty());
        reg.reset();
        assertTrue(reg.snapshot().isEmpty(), "snapshot should be empty after reset");

        // reset 后可以重新注册同名 counter
        reg.counter("c1").inc();
        assertEquals(1L, reg.counter("c1").count());
    }

    @Test
    void snapshot_isSortedByName() {
        reg.counter("z.counter").inc();
        reg.counter("a.counter").inc();
        reg.recordValue("m.gauge", 0.0);

        var snap = reg.snapshot();
        // 排序后第一个名字字典序最小
        for (int i = 1; i < snap.size(); i++) {
            assertTrue(snap.get(i - 1).name().compareTo(snap.get(i).name()) <= 0,
                    "snapshot not sorted at index " + i + ": "
                            + snap.get(i - 1).name() + " > " + snap.get(i).name());
        }
    }

    @Test
    void namespace_blank_fallsBackToKato() {
        PrometheusMetricsRegistry defaultNs = new PrometheusMetricsRegistry(new CollectorRegistry(), "");
        MetricsRegistry.CounterRef c = defaultNs.counter("test.counter");
        c.inc();
        assertTrue(defaultNs.snapshot().get(0).name().startsWith("kato_"),
                "blank namespace should fall back to kato, got: " + defaultNs.snapshot().get(0).name());
    }
}
