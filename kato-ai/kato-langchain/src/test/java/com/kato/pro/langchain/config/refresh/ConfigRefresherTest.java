package com.kato.pro.langchain.config.refresh;

import com.kato.pro.langchain.config.source.ConfigSnapshot;
import com.kato.pro.langchain.config.source.ConfigSource;
import com.kato.pro.langchain.config.source.LocalConfigSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * ConfigRefresher 行为测试。
 *
 * 注意：onEnvironmentChange 用 @EventListener — 这里直接走 refreshNow()
 * 验证 listener 分发逻辑，不模拟 Spring 事件（避免引入 ApplicationContext 复杂度）。
 */
class ConfigRefresherTest {

    private LocalConfigSource source;
    private ConfigRefresher refresher;
    private MockEnvironment env;

    @BeforeEach
    void setUp() {
        source = new LocalConfigSource();
        env = new MockEnvironment();
        env.setProperty("kato.audit.file.path", "/tmp/a.json");
        source.setEnvironment(env);
        refresher = new ConfigRefresher(source);
    }

    @Test
    void register_firesImmediatelyWithCurrentSnapshot() {
        AtomicInteger calls = new AtomicInteger();
        ConfigRefresher.Handle h = refresher.register(snap -> calls.incrementAndGet());
        assertEquals(1, calls.get());
        h.close();
    }

    @Test
    void refreshNow_incrementsVersionAndNotifies() {
        AtomicReference<ConfigSnapshot> seen = new AtomicReference<>();
        ConfigRefresher.Handle h = refresher.register(seen::set);
        long v0 = seen.get().version();

        env.setProperty("kato.audit.file.path", "/tmp/b.json");
        ConfigSnapshot fresh = refresher.refreshNow();

        assertNotNull(fresh);
        assertTrue(fresh.version() > v0);
        assertEquals("/tmp/b.json", fresh.get("kato.audit.file.path"));
        assertEquals("/tmp/b.json", seen.get().get("kato.audit.file.path"));
        h.close();
    }

    @Test
    void multipleListeners_allNotified() {
        AtomicInteger a = new AtomicInteger();
        AtomicInteger b = new AtomicInteger();
        ConfigRefresher.Handle ha = refresher.register(snap -> a.incrementAndGet());
        ConfigRefresher.Handle hb = refresher.register(snap -> b.incrementAndGet());

        env.setProperty("kato.x", "1");
        refresher.refreshNow();

        assertTrue(a.get() >= 1);
        assertTrue(b.get() >= 1);
        assertEquals(2, refresher.listenerCount());

        ha.close();
        hb.close();
    }

    @Test
    void listenerException_doesNotBlockOthers() {
        refresher.register(snap -> { throw new RuntimeException("boom"); });
        AtomicInteger good = new AtomicInteger();
        ConfigRefresher.Handle h = refresher.register(snap -> good.incrementAndGet());

        env.setProperty("kato.y", "1");
        refresher.refreshNow();

        assertTrue(good.get() >= 1);
        h.close();
    }

    @Test
    void close_removesListener() {
        AtomicInteger calls = new AtomicInteger();
        ConfigRefresher.Handle h = refresher.register(snap -> calls.incrementAndGet());
        int initial = calls.get();

        h.close();
        env.setProperty("kato.z", "1");
        refresher.refreshNow();

        assertEquals(initial, calls.get());
    }

    @Test
    void snapshotContainsSourceName() {
        AtomicReference<String> sourceName = new AtomicReference<>();
        ConfigRefresher.Handle h = refresher.register(snap -> sourceName.set(snap.source()));
        assertEquals("local", sourceName.get());
        h.close();
    }

    @Test
    void customConfigSource_isAccepted() {
        // 模拟一个自定义 ConfigSource（未来 Nacos 实现即此形态）
        ConfigSource custom = new ConfigSource() {
            @Override
            public String sourceName() {
                return "test-stub";
            }

            @Override
            public ConfigSnapshot getSnapshot() {
                return new ConfigSnapshot(Map.of("kato.x", "stub"), "test-stub", 1L, 0L);
            }

            @Override
            public AutoCloseable onChange(java.util.function.Consumer<ConfigSnapshot> callback) {
                return NO_OP_HANDLE;
            }
        };
        ConfigRefresher r = new ConfigRefresher(custom);
        AtomicReference<ConfigSnapshot> seen = new AtomicReference<>();
        ConfigRefresher.Handle h = r.register(seen::set);
        assertEquals("stub", seen.get().get("kato.x"));
        h.close();
    }
}
