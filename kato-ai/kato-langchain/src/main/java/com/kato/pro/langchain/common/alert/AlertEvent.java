package com.kato.pro.langchain.common.alert;

import com.kato.pro.langchain.common.metrics.MetricRegistry;

import java.time.Instant;
import java.util.List;

/**
 * 告警事件（spec §6 M11）。
 */
public record AlertEvent(
        String ruleName,
        AlertSeverity severity,
        String message,
        List<MetricRegistry.MetricSnapshot> triggeredBy,
        Instant occurredAt
) {
    public static AlertEvent of(AlertRule rule,
                                 MetricRegistry.MetricSnapshot snapshot,
                                 List<MetricRegistry.MetricSnapshot> all) {
        return new AlertEvent(
                rule.name(),
                rule.severity(),
                rule.message(snapshot),
                all,
                Instant.now()
        );
    }
}
