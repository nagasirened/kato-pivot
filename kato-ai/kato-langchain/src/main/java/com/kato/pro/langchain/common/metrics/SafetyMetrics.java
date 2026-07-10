package com.kato.pro.langchain.common.metrics;

/**
 * 内容安全关键路径埋点（spec §6 M11）。
 */
public final class SafetyMetrics {

    private SafetyMetrics() {}

    public static void onCheck(String kind, String result) {
        // kind: input/output; result: pass/reject/fallback
        MetricRegistry.counter("safety.check.total",
                "kind=" + kind + ",result=" + result).inc();
    }

    public static void onReject(String kind) {
        MetricRegistry.counter("safety.reject.total", "kind=" + kind).inc();
    }
}
