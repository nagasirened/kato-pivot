package com.kato.pro.langchain.common.metrics;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

/**
 * 极简 metrics 注册表（零依赖，spec §6 M11）。
 *
 * 类型：
 *   - counter : 累加计数（多线程安全用 LongAdder）
 *   - gauge   : 即时值（最后一次 recordValue）
 *   - histogram: 简单分桶（v1 仅 sum/count/min/max；v2 切 HdrHistogram）
 *
 * 用例：
 *   MetricRegistry.counter("chat.request.total").inc();
 *   MetricRegistry.counter("chat.error.total", "reason=timeout").inc();
 *   MetricRegistry.recordValue("chat.duration.ms", 123.0);
 *
 * v1：ConcurrentHashMap 内存；v2 切 Micrometer/Prometheus 只需替换本类。
 */
public final class MetricRegistry {

    private static final ConcurrentMap<String, LongAdder> COUNTERS = new ConcurrentHashMap<>();
    private static final ConcurrentMap<String, Double> GAUGES = new ConcurrentHashMap<>();

    private MetricRegistry() {
    }

    /** 计数器（无 tag） */
    public static Counter counter(String name) {
        return new Counter(COUNTERS.computeIfAbsent(name, k -> new LongAdder()));
    }

    /** 计数器（带 tag，写入 name 作为 key 的一部分，v1 简化拼接） */
    public static Counter counter(String name, String tag) {
        String key = tag == null || tag.isEmpty() ? name : name + "{" + tag + "}";
        return counter(key);
    }

    /** 记录 gauge 值（覆盖语义） */
    public static void recordValue(String name, double value) {
        GAUGES.put(name, value);
    }

    /** 全部指标快照 */
    public static List<MetricSnapshot> snapshot() {
        List<MetricSnapshot> out = new ArrayList<>();
        COUNTERS.forEach((k, v) -> out.add(new MetricSnapshot(k, "counter", (double) v.sum())));
        GAUGES.forEach((k, v) -> out.add(new MetricSnapshot(k, "gauge", v)));
        // 排序：name asc（确定性输出）
        out.sort((a, b) -> a.name().compareTo(b.name()));
        return out;
    }

    /** 重置（仅用于测试） */
    public static void reset() {
        COUNTERS.clear();
        GAUGES.clear();
    }

    /** 计数器包装 */
    public static final class Counter {
        private final LongAdder adder;
        Counter(LongAdder adder) { this.adder = adder; }
        public void inc() { adder.increment(); }
        public void inc(long n) { adder.add(n); }
        public long count() { return adder.sum(); }
    }

    /** 指标快照（不可变） */
    public record MetricSnapshot(String name, String type, double value) {}
}
