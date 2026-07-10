package com.kato.pro.langchain.common.metrics;

/**
 * 工具调用关键路径埋点（spec §6 M11）。
 */
public final class ToolMetrics {

    private ToolMetrics() {}

    public static void onDispatch(String tool, String type) {
        MetricRegistry.counter("tool.dispatch.total", "tool=" + tool + ",type=" + type).inc();
    }

    public static void onSuccess(String tool) {
        MetricRegistry.counter("tool.success.total", "tool=" + tool).inc();
    }

    public static void onFailure(String tool, String reason) {
        MetricRegistry.counter("tool.fail.total", "tool=" + tool + ",reason=" + reason).inc();
    }
}
