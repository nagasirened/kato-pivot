package com.kato.pro.langchain.config.source;

import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.config.listener.Listener;
import com.alibaba.nacos.api.exception.NacosException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * NacosConfigSource 行为测试 — 用 JDK 动态代理桩 ConfigService（避免 mockito 沙箱不兼容）。
 *
 * 沙箱不真连 Nacos。验证：
 *   1. 启动期拉初始配置（getConfig）并解析为 ConfigSnapshot
 *   2. 注册 Nacos listener（addListener）
 *   3. 模拟推送 → receiveConfigInfo 触发 → onChange callback 拿到新快照
 *   4. close() 时移除 listener + shutdown
 *   5. 异常隔离：getConfig 抛错时仍能用其他 dataId
 *   6. 解析 YAML 嵌套结构
 */
class NacosConfigSourceTest {

    /** 桩 ConfigService — 记录所有调用 + 返回可配置值。 */
    static class StubConfigService implements InvocationHandler {
        final Map<String, String> configByDataId = new HashMap<>();
        final List<String> addListenerCalls = new ArrayList<>();
        final List<String> removeListenerCalls = new ArrayList<>();
        boolean shutDownCalled = false;
        // 给定 dataId 抛 NacosException
        final java.util.Set<String> failingDataIds = new java.util.HashSet<>();
        // 记录注册到哪个 dataId 的 listener
        final Map<String, Listener> registeredListeners = new HashMap<>();

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            String name = method.getName();
            switch (name) {
                case "getConfig": {
                    String dataId = (String) args[0];
                    if (failingDataIds.contains(dataId)) {
                        throw new NacosException(500, "simulated failure for " + dataId);
                    }
                    return configByDataId.getOrDefault(dataId, "");
                }
                case "addListener": {
                    String dataId = (String) args[0];
                    Listener l = (Listener) args[2];
                    addListenerCalls.add(dataId);
                    registeredListeners.put(dataId, l);
                    return null;
                }
                case "removeListener": {
                    String dataId = (String) args[0];
                    removeListenerCalls.add(dataId);
                    return null;
                }
                case "publishConfig":
                case "publishConfigCas":
                case "removeConfig":
                    return true;
                case "getServerStatus":
                    return "UP";
                case "shutDown":
                    shutDownCalled = true;
                    return null;
                default:
                    return null;
            }
        }

        ConfigService asProxy() {
            return (ConfigService) Proxy.newProxyInstance(
                    ConfigService.class.getClassLoader(),
                    new Class<?>[] { ConfigService.class },
                    this);
        }

        /** 模拟 Nacos 推送：调用 registerListener.receiveConfigInfo。 */
        void push(String dataId, String configContent) {
            Listener l = registeredListeners.get(dataId);
            if (l != null) l.receiveConfigInfo(configContent);
        }
    }

    private StubConfigService stub;
    private NacosConfigSource source;
    private NacosConfigProperties properties;

    @BeforeEach
    void setUp() {
        properties = new NacosConfigProperties();
        properties.setEnabled(true);
        properties.setServerAddr("127.0.0.1:8848");
        properties.setDataIds(List.of("kato-langchain-common", "kato-langchain-rag"));
        properties.setGroup("DEFAULT_GROUP");
        properties.setTimeoutMs(5000L);
        stub = new StubConfigService();
    }

    @AfterEach
    void tearDown() {
        if (source != null) source.close();
    }

    @Test
    void sourceName_isNacos() {
        source = new NacosConfigSource(properties, stub.asProxy());
        assertEquals("nacos", source.sourceName());
    }

    @Test
    void initialize_pullsAllDataIdsAndRegistersListeners() {
        stub.configByDataId.put("kato-langchain-common",
                "kato:\n  audit:\n    file:\n      enabled: true\n      path: /var/log/audit.json\n");
        stub.configByDataId.put("kato-langchain-rag",
                "chat-engine:\n  rag-top-k: 8\n  rag-min-score: 0.7\n");

        source = new NacosConfigSource(properties, stub.asProxy());

        // 两个 dataId 都被拉过
        assertTrue(stub.addListenerCalls.contains("kato-langchain-common"));
        assertTrue(stub.addListenerCalls.contains("kato-langchain-rag"));
        assertEquals(2, stub.addListenerCalls.size());

        ConfigSnapshot snap = source.getSnapshot();
        assertNotNull(snap);
        assertEquals("nacos", snap.source());
        assertEquals("/var/log/audit.json", snap.get("kato.audit.file.path"));
        assertEquals("true", snap.get("kato.audit.file.enabled"));
        assertEquals("8", snap.get("chat-engine.rag-top-k"));
        assertEquals("0.7", snap.get("chat-engine.rag-min-score"));
        assertTrue(snap.version() > 0);
    }

    @Test
    void pushReceives_newSnapshotDelivered() {
        stub.configByDataId.put("kato-langchain-common", "");
        stub.configByDataId.put("kato-langchain-rag", "");

        source = new NacosConfigSource(properties, stub.asProxy());
        long v0 = source.getSnapshot().version();

        // 模拟 Nacos 推送
        stub.push("kato-langchain-common",
                "kato:\n  audit:\n    file:\n      path: /var/log/audit-rotated.json\n");

        ConfigSnapshot snap = source.getSnapshot();
        assertEquals("/var/log/audit-rotated.json", snap.get("kato.audit.file.path"));
        assertTrue(snap.version() > v0, "version should advance after push");
    }

    @Test
    void pushReceives_triggersOnChangeCallback() {
        stub.configByDataId.put("kato-langchain-common", "");
        stub.configByDataId.put("kato-langchain-rag", "");

        source = new NacosConfigSource(properties, stub.asProxy());

        AtomicInteger calls = new AtomicInteger();
        AtomicReference<ConfigSnapshot> seen = new AtomicReference<>();
        AutoCloseable handle = source.onChange(snap -> {
            calls.incrementAndGet();
            seen.set(snap);
        });
        int initial = calls.get();
        assertEquals(1, initial, "onChange fires immediately with current snapshot");

        // 模拟 Nacos 推送
        stub.push("kato-langchain-common", "chat-engine:\n  rag-top-k: 12\n");

        assertTrue(calls.get() > initial, "onChange should fire after push");
        assertEquals("12", seen.get().get("chat-engine.rag-top-k"));
        try { handle.close(); } catch (Exception ignore) {}
    }

    @Test
    void initialize_emptyDataIds_skipsNacosCalls() {
        properties.setDataIds(List.of());
        source = new NacosConfigSource(properties, stub.asProxy());

        assertEquals(0, stub.addListenerCalls.size());
        assertNotNull(source.getSnapshot());
        assertTrue(source.getSnapshot().values().isEmpty());
    }

    @Test
    void initialize_oneDataIdFails_otherSucceeds() {
        // 第一个 dataId 抛异常，第二个正常返回
        stub.failingDataIds.add("kato-langchain-common");
        stub.configByDataId.put("kato-langchain-rag",
                "chat-engine:\n  rag-top-k: 9\n");

        source = new NacosConfigSource(properties, stub.asProxy());

        // 失败不阻断 — 第二个 dataId 仍应加载
        ConfigSnapshot snap = source.getSnapshot();
        assertEquals("9", snap.get("chat-engine.rag-top-k"));
        // 两个 listener 都尝试注册
        assertEquals(2, stub.addListenerCalls.size());
    }

    @Test
    void close_unregistersListenersAndShutsDown() {
        stub.configByDataId.put("kato-langchain-common", "");
        stub.configByDataId.put("kato-langchain-rag", "");
        source = new NacosConfigSource(properties, stub.asProxy());

        source.close();

        assertEquals(2, stub.removeListenerCalls.size());
        assertTrue(stub.shutDownCalled, "shutDown should be called on close");
    }

    @Test
    void parseConfig_emptyYamlProducesEmptySnapshot() {
        properties.setDataIds(List.of("kato-langchain-empty"));
        stub.configByDataId.put("kato-langchain-empty", "");
        source = new NacosConfigSource(properties, stub.asProxy());

        ConfigSnapshot snap = source.getSnapshot();
        assertNotNull(snap);
        assertTrue(snap.values().isEmpty());
    }

    @Test
    void listenerException_doesNotPropagate() {
        stub.configByDataId.put("kato-langchain-common", "");
        stub.configByDataId.put("kato-langchain-rag", "");
        source = new NacosConfigSource(properties, stub.asProxy());

        source.onChange(snap -> { throw new RuntimeException("subscriber boom"); });
        AtomicInteger goodCalls = new AtomicInteger();
        source.onChange(snap -> goodCalls.incrementAndGet());

        // 推送时 — 第一个 subscriber 抛错，第二个仍收到
        stub.push("kato-langchain-common", "chat-engine:\n  rag-top-k: 1\n");
        assertTrue(goodCalls.get() >= 1);
    }
}
