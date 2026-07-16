package com.kato.pro.metrics;

import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.Counter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Prometheus 实现的 MetricsRegistry（spec §6 M12）。
 *
 * 内部把 io.prometheus.client.Counter 包成 MetricsRegistry.CounterRef 暴露给业务；
 * 同时保留原 Counter 引用在 unregister 时使用。
 */
@Slf4j
public class PrometheusMetricsRegistry implements MetricsRegistry {

    private final CollectorRegistry registry;
    private final String namespace;
    private final ConcurrentMap<String, Counter> promCounters = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Double> gauges = new ConcurrentHashMap<>();

    public PrometheusMetricsRegistry(CollectorRegistry registry, String namespace) {
        this.registry = registry;
        this.namespace = namespace == null || namespace.isBlank() ? "kato" : namespace;
    }

    @Override
    public MetricsRegistry.CounterRef counter(String name) {
        return counter(name, null);
    }

    @Override
    public MetricsRegistry.CounterRef counter(String name, String tags) {
        // Prometheus 指标名规范 [a-zA-Z_:][a-zA-Z0-9_:]*，业务常用 . 表达层级 → 转 _
        String fullName = sanitizeMetricName(namespace + "_" + name);
        String key = tags == null || tags.isBlank() ? fullName : fullName + "|" + tags;
        io.prometheus.client.Counter prom = promCounters.computeIfAbsent(key, k -> buildCounter(fullName, tags));
        return new MetricsRegistry.CounterRef() {
            @Override public void inc() { prom.inc(); }
            @Override public void inc(long n) { prom.inc(n); }
            @Override public long count() { return (long) prom.get(); }
        };
    }

    @Override
    public void recordValue(String name, double value) {
        gauges.put(namespace + "_" + name, value);
    }

    @Override
    public List<MetricSnapshot> snapshot() {
        List<MetricSnapshot> out = new ArrayList<>();
        for (Map.Entry<String, Counter> e : promCounters.entrySet()) {
            out.add(new MetricSnapshot(e.getKey(), "counter", e.getValue().get()));
        }
        for (Map.Entry<String, Double> e : gauges.entrySet()) {
            out.add(new MetricSnapshot(e.getKey(), "gauge", e.getValue()));
        }
        out.sort((a, b) -> a.name().compareTo(b.name()));
        return out;
    }

    /**
     * Prometheus 指标名规范化：把 "." 转成 "_"，保留其它合法字符。
     * 业务常用 "chat.requests" / "http.requests" 这种命名，进 collector 前必须转换。
     */
    static String sanitizeMetricName(String name) {
        if (name == null || name.isEmpty()) return name;
        StringBuilder sb = new StringBuilder(name.length());
        for (int i = 0; i < name.length(); i++) {
            char ch = name.charAt(i);
            if (ch == '.') sb.append('_');
            else sb.append(ch);
        }
        return sb.toString();
    }

    @Override
    public void reset() {
        for (Counter c : promCounters.values()) {
            try { registry.unregister(c); } catch (Exception ignored) {}
        }
        promCounters.clear();
        gauges.clear();
    }

    private Counter buildCounter(String fullName, String tags) {
        // simpleclient 0.16 API: labelNames(String...) + labelValues(String...)（调用时传入）
        // 同一 (name, labelNames) 组合重复 register 会抛 IllegalArgumentException，
        // 我们用 key 唯一化（key = fullName + "|" + tags）由 computeIfAbsent 兜底
        if (tags == null || tags.isBlank()) {
            return Counter.build().name(fullName).help(fullName).register(registry);
        }
        String[] labelNames = new String[]{};
        String[] labelValues = new String[]{};
        java.util.List<String> names = new java.util.ArrayList<>();
        java.util.List<String> values = new java.util.ArrayList<>();
        for (String kv : tags.split(",")) {
            int eq = kv.indexOf('=');
            if (eq > 0) {
                names.add(kv.substring(0, eq).trim());
                values.add(kv.substring(eq + 1).trim());
            }
        }
        labelNames = names.toArray(new String[0]);
        labelValues = values.toArray(new String[0]);
        // 简单做法：不使用 labels（simpleclient 0.16 API 限制），把 tag 拼到 name 后缀
        // 同 (name, tags) 第二次 register 时 computeIfAbsent 已跳过
        String taggedName = sanitizeMetricName(fullName + "_" + String.join("_", labelValues));
        return Counter.build().name(taggedName).help(fullName).register(registry);
    }
}
