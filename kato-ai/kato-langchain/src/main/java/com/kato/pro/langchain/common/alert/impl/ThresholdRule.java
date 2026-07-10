package com.kato.pro.langchain.common.alert.impl;

import com.kato.pro.langchain.common.alert.AlertRule;
import com.kato.pro.langchain.common.alert.AlertSeverity;
import com.kato.pro.langchain.common.metrics.MetricRegistry;
import lombok.Value;

/**
 * 阈值告警规则：指标名匹配 + value 超过 threshold 触发。
 */
@Value
public class ThresholdRule implements AlertRule {

    String name;
    String metricNamePattern;
    double threshold;
    AlertSeverity severity;
    String description;

    @Override
    public AlertSeverity severity() { return this.severity; }

    @Override
    public String name() { return this.name; }

    @Override
    public boolean evaluate(MetricRegistry.MetricSnapshot snapshot) {
        if (!snapshot.name().startsWith(metricNamePattern)) return false;
        return snapshot.value() >= threshold;
    }

    @Override
    public String message(MetricRegistry.MetricSnapshot snapshot) {
        return String.format("[%s] %s = %.0f >= threshold=%.0f (%s)",
                severity, snapshot.name(), snapshot.value(), threshold, description);
    }
}
