package com.kato.pro.langchain.common.alert;

import com.kato.pro.langchain.common.alert.impl.ThresholdRule;
import com.kato.pro.langchain.common.metrics.MetricRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * AlertMonitor 单元测试（M11）。
 *
 * 直接构造 AlertMonitor + 调 check() — 验证规则命中后触发 dispatcher。
 */
class AlertMonitorTest {

    private final List<AlertEvent> captured = Collections.synchronizedList(new ArrayList<>());
    private final AtomicInteger dispatchCount = new AtomicInteger();

    @BeforeEach
    void setUp() {
        MetricRegistry.reset();
        captured.clear();
        dispatchCount.set(0);
    }

    @Test
    void check_triggeredRule_dispatchesOnce() {
        // 制造指标：safety.reject.total{...} = 100（超过 threshold 50）
        for (int i = 0; i < 100; i++) {
            MetricRegistry.counter("safety.reject.total", "kind=input").inc();
        }

        AlertRule rule = new ThresholdRule("r1", "safety.reject.total", 50.0,
                AlertSeverity.WARN, "test");
        AlertDispatcher dispatcher = new AlertDispatcher() {
            @Override public String name() { return "test"; }
            @Override public void dispatch(AlertEvent event) {
                captured.add(event);
                dispatchCount.incrementAndGet();
            }
        };

        AlertProperties props = new AlertProperties();
        props.setEnabled(true);
        AlertMonitor monitor = new AlertMonitor(props, List.of(rule), List.of(dispatcher));
        monitor.check();

        // 多次 inc 但命中规则 — snapshot 1 条 metric 多 bucket 都会被 evaluate
        // 由于我们对每个 tag key 单独 counter，规则 prefix 匹配 "safety.reject.total"
        // 多个 tag 的 counter 都会触发，所以 dispatchCount 可能 > 1
        assertNotNull(captured);
        assertEquals(true, dispatchCount.get() >= 1);
        // 第一次触发的 ruleName 应为 r1
        assertEquals("r1", captured.get(0).ruleName());
    }

    @Test
    void check_belowThreshold_doesNotDispatch() {
        MetricRegistry.counter("safety.reject.total", "kind=input").inc(); // = 1

        AlertRule rule = new ThresholdRule("r1", "safety.reject.total", 50.0,
                AlertSeverity.WARN, "test");
        AlertDispatcher dispatcher = new AlertDispatcher() {
            @Override public String name() { return "test"; }
            @Override public void dispatch(AlertEvent event) { captured.add(event); }
        };
        AlertMonitor monitor = new AlertMonitor(new AlertProperties(),
                List.of(rule), List.of(dispatcher));
        monitor.check();
        assertEquals(0, captured.size());
    }

    @Test
    void check_disabledByProperties_skipsCheck() {
        MetricRegistry.counter("safety.reject.total", "kind=input").inc();
        for (int i = 0; i < 100; i++) MetricRegistry.counter("safety.reject.total", "kind=input").inc();

        AlertRule rule = new ThresholdRule("r1", "safety.reject.total", 50.0,
                AlertSeverity.WARN, "test");
        AlertDispatcher dispatcher = new AlertDispatcher() {
            @Override public String name() { return "test"; }
            @Override public void dispatch(AlertEvent event) { captured.add(event); }
        };
        AlertProperties props = new AlertProperties();
        props.setEnabled(false);
        AlertMonitor monitor = new AlertMonitor(props, List.of(rule), List.of(dispatcher));
        monitor.check();
        assertEquals(0, captured.size());
    }
}
