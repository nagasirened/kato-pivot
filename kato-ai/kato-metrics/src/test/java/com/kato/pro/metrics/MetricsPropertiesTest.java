package com.kato.pro.metrics;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * MetricsProperties 默认值测试（spec §6 M12）。
 *
 * 验证默认值符合预期；业务可通过 application.yml 覆盖。
 */
class MetricsPropertiesTest {

    @Test
    void defaults_areSensible() {
        MetricsProperties p = new MetricsProperties();
        assertTrue(p.isEnabled(), "metrics should be enabled by default");
        assertEquals("/actuator/prometheus", p.getPath());
        assertEquals("kato", p.getNamespace());
    }

    @Test
    void setters_roundTripValues() {
        MetricsProperties p = new MetricsProperties();
        p.setEnabled(false);
        p.setPath("/custom/prom");
        p.setNamespace("myservice");

        assertFalse(p.isEnabled());
        assertEquals("/custom/prom", p.getPath());
        assertEquals("myservice", p.getNamespace());
    }

    @Test
    void metricsRegistry_canBeCreatedWithCustomNamespace() {
        // 验证 props 与 registry 协作
        MetricsProperties p = new MetricsProperties();
        p.setNamespace("custom");

        PrometheusMetricsRegistry reg =
                new PrometheusMetricsRegistry(new io.prometheus.client.CollectorRegistry(), p.getNamespace());
        reg.counter("foo").inc();

        var snap = reg.snapshot();
        assertTrue(snap.get(0).name().startsWith("custom_"),
                "namespace should prefix metric name, got: " + snap.get(0).name());
    }
}
