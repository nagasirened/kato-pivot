package com.kato.pro.metrics;

import java.util.List;

/**
 * 通用 metrics 注册表接口（spec §6 M12）。
 *
 * 业务通过本接口埋点，kato-metrics 提供 Prometheus 实现；
 * 任何 kato-pivot 子模块（langchain / sensitive / recommend）都可使用。
 *
 * 实现：
 *   - PrometheusMetricsRegistry（kato-metrics 模块，基于 simpleclient）
 *   - SimpleMetricRegistry（kato-langchain fallback，零依赖）
 */
public interface MetricsRegistry {

    /** 计数器（无 tag） */
    CounterRef counter(String name);

    /** 计数器（带 tag；tag 格式 "k1=v1,k2=v2"） */
    CounterRef counter(String name, String tags);

    /** 记录 gauge 值（覆盖语义） */
    void recordValue(String name, double value);

    /** 全部指标快照（用于单元测试 + /metrics JSON 端点） */
    List<MetricSnapshot> snapshot();

    /** 重置（仅用于测试） */
    void reset();

    /** 计数器接口 */
    interface CounterRef {
        void inc();
        void inc(long n);
        long count();
    }

    /** 指标快照（不可变） */
    record MetricSnapshot(String name, String type, double value) {}
}
