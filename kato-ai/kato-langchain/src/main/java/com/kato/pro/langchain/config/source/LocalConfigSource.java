package com.kato.pro.langchain.config.source;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * 本地配置源（默认实现）。
 *
 * 行为：
 *   - getSnapshot() 从 Environment 抓取所有 kato.* / chat-engine.* / rag.* / model.*
 *     / sync.* / jwt.* / alert.* 关键 namespace 的属性
 *   - onChange(callback) 监听 EnvironmentChangeEvent（Spring 内置）
 *     → 重新拉快照 → 触发 callback
 *
 * 沙箱友好：完全基于 Environment，不需要外部 server。
 * M17+ 接入 Nacos：替换为 NacosConfigSource 即可，ConfigRefresher / 业务方 listener 不动。
 */
@Slf4j
@Component
public class LocalConfigSource implements ConfigSource, EnvironmentAware {

    public static final String SOURCE_NAME = "local";

    /** 关注的关键 namespace 列表（性能 + 信噪比权衡）。 */
    public static final List<String> WATCHED_NAMESPACES = List.of(
            "kato.", "chat-engine.", "rag.", "model.", "sync.", "jwt.", "alert."
    );

    private final AtomicReference<ConfigSnapshot> current = new AtomicReference<>(ConfigSnapshot.empty());
    private final AtomicLong versionSeq = new AtomicLong(0);
    private final List<Consumer<ConfigSnapshot>> listeners = new CopyOnWriteArrayList<>();

    private volatile ConfigurableEnvironment environment;

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = (ConfigurableEnvironment) environment;
        refresh();
    }

    @Override
    public String sourceName() {
        return SOURCE_NAME;
    }

    @Override
    public ConfigSnapshot getSnapshot() {
        return current.get();
    }

    /**
     * 主动触发一次重新抓取（被 EnvironmentChangeEvent 监听器调用）。
     */
    public ConfigSnapshot refresh() {
        if (environment == null) {
            log.debug("Environment not set yet, skip refresh");
            return current.get();
        }
        Map<String, String> collected = collectWatchedProperties(environment);
        long v = versionSeq.incrementAndGet();
        ConfigSnapshot fresh = new ConfigSnapshot(collected, SOURCE_NAME, v, System.currentTimeMillis());
        current.set(fresh);
        log.debug("LocalConfigSource refreshed: version={} keys={}", v, collected.size());
        notifyListeners(fresh);
        return fresh;
    }

    @Override
    public AutoCloseable onChange(Consumer<ConfigSnapshot> callback) {
        if (callback == null) return NO_OP_HANDLE;
        listeners.add(callback);
        // 立即触发一次（让新订阅者拿到当前快照）
        try {
            callback.accept(current.get());
        } catch (Exception e) {
            log.warn("Initial callback failed: {}", e.getMessage());
        }
        return () -> listeners.remove(callback);
    }

    private void notifyListeners(ConfigSnapshot snapshot) {
        for (Consumer<ConfigSnapshot> listener : listeners) {
            try {
                listener.accept(snapshot);
            } catch (Exception e) {
                log.warn("Listener failed: {}", e.getMessage(), e);
            }
        }
    }

    /**
     * 从 Environment 抓取所有匹配 WATCHED_NAMESPACES 的属性。
     * 按 key 排序保证快照稳定性。
     */
    static Map<String, String> collectWatchedProperties(Environment env) {
        Map<String, String> result = new LinkedHashMap<>();
        MutablePropertySources sources = (env instanceof ConfigurableEnvironment ce)
                ? ce.getPropertySources() : null;
        if (sources == null) {
            // 退化：只扫 systemProperties + systemEnvironment
            collectFromSingleSource("systemProperties", System.getProperties(), result);
            collectFromSingleSource("systemEnvironment", System.getenv(), result);
            return result;
        }
        for (PropertySource<?> ps : sources) {
            if (ps instanceof MapPropertySource mps) {
                collectFromSingleSource(ps.getName(), mps.getSource(), result);
            }
        }
        return result;
    }

    private static void collectFromSingleSource(String sourceName, Object source, Map<String, String> out) {
        if (!(source instanceof Map<?, ?> map)) return;
        for (Map.Entry<?, ?> e : map.entrySet()) {
            Object key = e.getKey();
            if (!(key instanceof String s)) continue;
            if (!matchesAny(s)) continue;
            Object val = e.getValue();
            out.put(s, val == null ? null : val.toString());
        }
    }

    private static boolean matchesAny(String key) {
        for (String ns : WATCHED_NAMESPACES) {
            if (key.startsWith(ns)) return true;
        }
        return false;
    }

    /**
     * 单元测试辅助：清空当前状态。
     */
    void reset() {
        current.set(ConfigSnapshot.empty());
        versionSeq.set(0);
        listeners.clear();
    }
}
