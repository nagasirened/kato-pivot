package com.kato.pro.langchain.common.metrics;

/**
 * ChatEngine 关键路径埋点（spec §6 M11）。
 *
 * 所有埋点都是 best-effort — 不抛错，不影响主业务。
 */
public final class ChatMetrics {

    private ChatMetrics() {}

    public static void onRequest(String intent) {
        MetricRegistry.counter("chat.request.total", "intent=" + (intent == null ? "unknown" : intent)).inc();
    }

    public static void onError(String reason) {
        MetricRegistry.counter("chat.error.total", "reason=" + (reason == null ? "unknown" : reason)).inc();
    }

    public static void onDuration(long durationMs) {
        MetricRegistry.recordValue("chat.duration.ms", durationMs);
    }

    public static void onTokenUsage(int tokens) {
        MetricRegistry.counter("chat.token.total").inc(tokens);
    }
}
