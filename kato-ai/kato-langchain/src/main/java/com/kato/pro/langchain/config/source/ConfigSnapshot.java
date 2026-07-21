package com.kato.pro.langchain.config.source;

import java.util.Collections;
import java.util.Map;

/**
 * 配置快照 — 在某一时点所有关键配置的不可变视图。
 *
 * 字段说明：
 *   - values      : 完整 key→value 映射（kato.audit.file.path / chat-engine.rag-top-k / ...）
 *   - source      : 来源标识（local / nacos / apollo / custom）
 *   - version     : 单调递增的版本号（每次变更 +1，listener 据此判断是否需要 reload）
 *   - capturedAt  : 抓取快照的时间（epoch millis）
 *
 * 不可变：所有 field 构造后只读。线程安全：可任意线程共享。
 */
public record ConfigSnapshot(
        Map<String, String> values,
        String source,
        long version,
        long capturedAt
) {
    public ConfigSnapshot {
        values = values == null ? Map.of() : Collections.unmodifiableMap(values);
    }

    public static ConfigSnapshot empty() {
        return new ConfigSnapshot(Map.of(), "empty", 0L, System.currentTimeMillis());
    }

    public String get(String key) {
        return values == null ? null : values.get(key);
    }

    public String getOrDefault(String key, String defaultValue) {
        String v = get(key);
        return v == null ? defaultValue : v;
    }

    public boolean has(String key) {
        return values != null && values.containsKey(key);
    }
}
