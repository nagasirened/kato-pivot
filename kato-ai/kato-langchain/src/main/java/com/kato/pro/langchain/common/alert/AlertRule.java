package com.kato.pro.langchain.common.alert;

import com.kato.pro.langchain.common.metrics.MetricRegistry;

/**
 * 告警规则 SPI（M11）。
 *
 * 实现：基于单个指标快照判断是否触发告警。
 *
 * v1：单指标 + 简单阈值；v2 引入多指标复合规则 + 时间窗口。
 */
public interface AlertRule {

    /** 规则名（用于日志/告警去重） */
    String name();

    /** 评估某个指标快照 — 命中时返回 true */
    boolean evaluate(MetricRegistry.MetricSnapshot snapshot);

    /** 告警严重度 */
    AlertSeverity severity();

    /** 告警消息（包含当前值） */
    String message(MetricRegistry.MetricSnapshot snapshot);
}
