package com.kato.pro.langchain.common.alert.impl;

import com.kato.pro.langchain.common.alert.AlertRule;
import com.kato.pro.langchain.common.alert.AlertSeverity;
import com.kato.pro.langchain.common.metrics.MetricRegistry;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ThresholdRuleTest {

    @Test
    void evaluate_belowThreshold_returnsFalse() {
        AlertRule rule = new ThresholdRule("r1", "metric.x", 100.0, AlertSeverity.WARN, "test");
        assertFalse(rule.evaluate(new MetricRegistry.MetricSnapshot("metric.x", "counter", 50.0)));
    }

    @Test
    void evaluate_aboveThreshold_returnsTrue() {
        AlertRule rule = new ThresholdRule("r1", "metric.x", 100.0, AlertSeverity.WARN, "test");
        assertTrue(rule.evaluate(new MetricRegistry.MetricSnapshot("metric.x", "counter", 150.0)));
    }

    @Test
    void evaluate_metricNameNotMatch_returnsFalse() {
        AlertRule rule = new ThresholdRule("r1", "metric.x", 100.0, AlertSeverity.WARN, "test");
        assertFalse(rule.evaluate(new MetricRegistry.MetricSnapshot("other.metric", "counter", 999.0)));
    }

    @Test
    void message_includesMetricNameAndValue() {
        AlertRule rule = new ThresholdRule("r1", "metric.x", 100.0, AlertSeverity.CRITICAL, "test-desc");
        String msg = rule.message(new MetricRegistry.MetricSnapshot("metric.x", "counter", 200.0));
        assertTrue(msg.contains("metric.x"));
        assertTrue(msg.contains("200"));
        assertTrue(msg.contains("CRITICAL"));
    }
}
