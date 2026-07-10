package com.kato.pro.langchain.common.alert;

import com.kato.pro.langchain.common.metrics.MetricRegistry;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 告警监控器（spec §6 M11）。
 *
 *   - @Scheduled 周期检查（默认 60s）
 *   - 遍历所有 AlertRule.evaluate
 *   - 命中 → 调所有 AlertDispatcher.dispatch
 *   - 防抖：同一规则 5 分钟内只发一次（v1 简单 in-memory 去重）
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AlertMonitor {

    private final AlertProperties properties;
    private final List<AlertRule> rules;
    private final List<AlertDispatcher> dispatchers;

    /** ruleName -> last trigger epoch ms */
    private final Map<String, Long> lastTrigger = new ConcurrentHashMap<>();
    private static final long DEDUP_WINDOW_MS = 5 * 60_000L;

    @PostConstruct
    void init() {
        log.info("AlertMonitor initialized: rules={} dispatchers={} intervalMs={}",
                rules.size(), dispatchers.size(), properties.getIntervalMs());
    }

    @Scheduled(fixedDelayString = "${alert.interval-ms:60000}")
    public void check() {
        if (!properties.isEnabled()) return;
        List<MetricRegistry.MetricSnapshot> all = MetricRegistry.snapshot();
        for (AlertRule rule : rules) {
            for (MetricRegistry.MetricSnapshot snap : all) {
                if (rule.evaluate(snap)) {
                    fire(rule, snap, all);
                }
            }
        }
    }

    private void fire(AlertRule rule, MetricRegistry.MetricSnapshot snap, List<MetricRegistry.MetricSnapshot> all) {
        long now = System.currentTimeMillis();
        Long last = lastTrigger.get(rule.name());
        if (last != null && now - last < DEDUP_WINDOW_MS) {
            log.debug("Alert dedup skip: rule={} last={}", rule.name(), last);
            return;
        }
        lastTrigger.put(rule.name(), now);
        AlertEvent event = AlertEvent.of(rule, snap, all);
        for (AlertDispatcher d : dispatchers) {
            try {
                d.dispatch(event);
            } catch (Exception e) {
                log.warn("AlertDispatcher failed: dispatcher={} error={}", d.name(), e.getMessage());
            }
        }
    }
}
