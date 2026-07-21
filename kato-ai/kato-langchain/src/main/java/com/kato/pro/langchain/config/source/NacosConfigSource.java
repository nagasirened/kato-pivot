package com.kato.pro.langchain.config.source;

import com.alibaba.nacos.api.NacosFactory;
import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.config.listener.AbstractListener;
import com.alibaba.nacos.api.exception.NacosException;
import lombok.extern.slf4j.Slf4j;
import org.yaml.snakeyaml.Yaml;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * Nacos 配置源 — ConfigSource SPI 的生产实现。
 *
 * 行为：
 *   - 启动时按 dataIds 拉初始配置（合并到一个 ConfigSnapshot）
 *   - 每个 dataId 注册一个 Listener，收到推送时解析 → 重新合并 → 触发 onChange callback
 *   - 解析失败时保留上一份快照 + 记日志（fail-open）
 *   - close() 时移除所有 Listener + shutdown ConfigService
 *
 * 不依赖 spring-cloud-alibaba / spring-cloud-context：
 *   - 直接用 nacos-client:2.3.2 的 NacosFactory + ConfigService
 *   - M16 的 ConfigRefresher 透传 onChange callback → 业务方 listener 收到推送
 *
 * 沙箱测试：NacosConfigSourceTest 用 JDK 动态代理 mock ConfigService，避开真连。
 */
@Slf4j
public class NacosConfigSource implements ConfigSource {

    public static final String SOURCE_NAME = "nacos";

    private final NacosConfigProperties properties;
    private final ConfigService configService;
    private final AtomicReference<ConfigSnapshot> current = new AtomicReference<>(ConfigSnapshot.empty());
    private final AtomicLong versionSeq = new AtomicLong(0);
    private final Map<String, AbstractListener> listeners = new ConcurrentHashMap<>();
    private final CopyOnWriteArrayList<Consumer<ConfigSnapshot>> subscribers = new CopyOnWriteArrayList<>();
    private final Yaml yaml = new Yaml();

    public NacosConfigSource(NacosConfigProperties properties) {
        this(properties, createConfigService(properties));
    }

    /**
     * 构造函数 — 允许测试传入 mock ConfigService。
     */
    public NacosConfigSource(NacosConfigProperties properties, ConfigService configService) {
        this.properties = properties;
        this.configService = configService;
        initialize();
    }

    private static ConfigService createConfigService(NacosConfigProperties props) {
        try {
            Properties cfg = new Properties();
            cfg.put("serverAddr", props.getServerAddr());
            if (props.getNamespace() != null && !props.getNamespace().isEmpty()) {
                cfg.put("namespace", props.getNamespace());
            }
            if (props.getUsername() != null && !props.getUsername().isEmpty()) {
                cfg.put("username", props.getUsername());
            }
            if (props.getPassword() != null && !props.getPassword().isEmpty()) {
                cfg.put("password", props.getPassword());
            }
            return NacosFactory.createConfigService(cfg);
        } catch (NacosException e) {
            throw new IllegalStateException(
                    "Failed to create Nacos ConfigService (server=" + props.getServerAddr() + ")", e);
        }
    }

    /**
     * 初始化：拉初始配置 + 注册 Listener。
     */
    private void initialize() {
        List<String> dataIds = properties.getDataIds();
        if (dataIds == null || dataIds.isEmpty()) {
            log.warn("NacosConfigSource has no dataIds configured; snapshot will remain empty");
            return;
        }
        Map<String, String> merged = new LinkedHashMap<>();
        for (String dataId : dataIds) {
            // 拉取初始配置（容错：一个失败不阻塞其他）
            try {
                String content = configService.getConfig(dataId, properties.getGroup(), properties.getTimeoutMs());
                if (content != null && !content.isEmpty()) {
                    merged.putAll(parseConfig(content));
                }
            } catch (NacosException e) {
                log.warn("Failed to load dataId={} from Nacos: {}", dataId, e.getErrMsg());
            }
            // 注册 listener（独立 try，不被 getConfig 失败影响）
            try {
                registerListener(dataId, merged);
            } catch (Exception e) {
                log.warn("Failed to register listener for dataId={}: {}", dataId, e.getMessage());
            }
        }
        publishSnapshot(merged, "init");
    }

    private void registerListener(String dataId, Map<String, String> mergedStorage) {
        AbstractListener listener = new AbstractListener() {
            @Override
            public void receiveConfigInfo(String configInfo) {
                log.info("Nacos push received: dataId={} size={}", dataId,
                        configInfo == null ? 0 : configInfo.length());
                if (configInfo == null || configInfo.isEmpty()) {
                    return;
                }
                try {
                    Map<String, String> parsed = parseConfig(configInfo);
                    // 合并新解析结果到全局 merged，再 publish
                    Map<String, String> full = new LinkedHashMap<>(current.get().values());
                    full.putAll(parsed);
                    publishSnapshot(full, "nacos-push:" + dataId);
                } catch (Exception e) {
                    log.warn("Failed to parse Nacos push for dataId={}: {}", dataId, e.getMessage());
                }
            }
        };
        try {
            configService.addListener(dataId, properties.getGroup(), listener);
            listeners.put(dataId, listener);
            log.info("Nacos listener registered: dataId={} group={}", dataId, properties.getGroup());
        } catch (NacosException e) {
            log.warn("Failed to register listener for dataId={}: {}", dataId, e.getErrMsg());
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> parseConfig(String content) {
        Object obj = yaml.load(content);
        if (obj == null) return Map.of();
        if (obj instanceof Map<?, ?> map) {
            return flatten(null, (Map<String, Object>) map, new HashMap<>());
        }
        log.warn("Nacos config is not a YAML map: {}", obj.getClass());
        return Map.of();
    }

    @SuppressWarnings("unchecked")
    private static Map<String, String> flatten(String prefix, Map<String, Object> map, Map<String, String> out) {
        for (Map.Entry<String, Object> e : map.entrySet()) {
            String key = prefix == null ? e.getKey() : prefix + "." + e.getKey();
            Object v = e.getValue();
            if (v instanceof Map<?, ?> child) {
                flatten(key, (Map<String, Object>) child, out);
            } else if (v != null) {
                out.put(key, String.valueOf(v));
            }
        }
        return out;
    }

    private void publishSnapshot(Map<String, String> values, String reason) {
        long v = versionSeq.incrementAndGet();
        ConfigSnapshot fresh = new ConfigSnapshot(values, SOURCE_NAME, v, System.currentTimeMillis());
        current.set(fresh);
        log.info("Nacos snapshot published: reason={} version={} keys={}", reason, v, values.size());
        for (Consumer<ConfigSnapshot> sub : subscribers) {
            try {
                sub.accept(fresh);
            } catch (Exception e) {
                log.warn("Nacos subscriber failed: {}", e.getMessage(), e);
            }
        }
    }

    @Override
    public String sourceName() {
        return SOURCE_NAME;
    }

    @Override
    public ConfigSnapshot getSnapshot() {
        return current.get();
    }

    @Override
    public AutoCloseable onChange(Consumer<ConfigSnapshot> callback) {
        if (callback == null) return NO_OP_HANDLE;
        subscribers.add(callback);
        // 立即触发一次（让新订阅者拿到当前快照）
        try {
            callback.accept(current.get());
        } catch (Exception e) {
            log.warn("Initial Nacos callback failed: {}", e.getMessage());
        }
        return () -> subscribers.remove(callback);
    }

    /**
     * 释放资源：移除所有 listener + shutdown ConfigService。
     */
    public void close() {
        for (Map.Entry<String, AbstractListener> e : listeners.entrySet()) {
            try {
                configService.removeListener(e.getKey(), properties.getGroup(), e.getValue());
            } catch (Exception ex) {
                log.warn("removeListener failed: dataId={} err={}", e.getKey(), ex.getMessage());
            }
        }
        listeners.clear();
        try {
            configService.shutDown();
        } catch (NacosException e) {
            log.warn("ConfigService shutDown failed: {}", e.getErrMsg());
        }
    }
}
