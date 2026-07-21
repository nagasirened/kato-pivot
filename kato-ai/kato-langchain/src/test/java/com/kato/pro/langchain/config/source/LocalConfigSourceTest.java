package com.kato.pro.langchain.config.source;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * LocalConfigSource 行为测试。
 *
 * 不依赖 Spring 容器 — 手动 setEnvironment。
 */
class LocalConfigSourceTest {

    private LocalConfigSource source;
    private MockEnvironment env;

    @BeforeEach
    void setUp() {
        source = new LocalConfigSource();
        env = new MockEnvironment();
    }

    @AfterEach
    void tearDown() {
        source.reset();
    }

    @Test
    void sourceName_isLocal() {
        assertEquals("local", source.sourceName());
    }

    @Test
    void beforeEnvironment_initialSnapshotIsEmpty() {
        assertNotNull(source.getSnapshot());
        assertTrue(source.getSnapshot().values().isEmpty());
    }

    @Test
    void setEnvironment_picksUpWatchedProperties() {
        env.setProperty("kato.audit.file.path", "/tmp/audit.json");
        env.setProperty("kato.audit.file.enabled", "true");
        env.setProperty("chat-engine.rag-top-k", "8");
        env.setProperty("rag.retriever.top-k", "10");
        env.setProperty("unrelated.namespace.foo", "skip-me");
        env.setProperty("spring.datasource.url", "skip-me-too");

        source.setEnvironment(env);
        ConfigSnapshot snap = source.getSnapshot();

        assertEquals("/tmp/audit.json", snap.get("kato.audit.file.path"));
        assertEquals("true", snap.get("kato.audit.file.enabled"));
        assertEquals("8", snap.get("chat-engine.rag-top-k"));
        assertEquals("10", snap.get("rag.retriever.top-k"));
        assertFalse(snap.has("unrelated.namespace.foo"));
        assertFalse(snap.has("spring.datasource.url"));
    }

    @Test
    void refresh_incrementsVersion() {
        env.setProperty("kato.x", "1");
        source.setEnvironment(env);
        long v1 = source.getSnapshot().version();

        env.setProperty("kato.x", "2");
        ConfigSnapshot s2 = source.refresh();
        assertTrue(s2.version() > v1);
        assertEquals("2", s2.get("kato.x"));
    }

    @Test
    void onChange_firesImmediatelyWithCurrentSnapshot() {
        env.setProperty("kato.y", "1");
        source.setEnvironment(env);

        AtomicInteger calls = new AtomicInteger();
        StringBuilder seen = new StringBuilder();
        AutoCloseable handle = source.onChange(snap -> {
            calls.incrementAndGet();
            seen.append(snap.get("kato.y")).append(",");
        });

        assertEquals(1, calls.get());
        assertEquals("1,", seen.toString());
        try { handle.close(); } catch (Exception ignore) {}
    }

    @Test
    void onChange_firesOnRefresh() {
        env.setProperty("kato.z", "1");
        source.setEnvironment(env);

        AtomicInteger calls = new AtomicInteger();
        AutoCloseable handle = source.onChange(snap -> calls.incrementAndGet());
        int after = calls.get();

        env.setProperty("kato.z", "2");
        source.refresh();

        assertEquals(after + 1, calls.get());
        try { handle.close(); } catch (Exception ignore) {}
    }

    @Test
    void onChange_closeRemovesListener() {
        env.setProperty("kato.q", "1");
        source.setEnvironment(env);

        AtomicInteger calls = new AtomicInteger();
        AutoCloseable handle = source.onChange(snap -> calls.incrementAndGet());
        int initial = calls.get();
        try { handle.close(); } catch (Exception ignore) {}

        env.setProperty("kato.q", "2");
        source.refresh();
        assertEquals(initial, calls.get(), "listener should not fire after close");
    }

    @Test
    void listenerException_doesNotPropagate() {
        env.setProperty("kato.w", "1");
        source.setEnvironment(env);

        AtomicInteger goodCalls = new AtomicInteger();
        source.onChange(snap -> { throw new RuntimeException("boom"); });
        source.onChange(snap -> goodCalls.incrementAndGet());

        // refresh — 第一个 listener 抛错，第二个仍应触发
        env.setProperty("kato.w", "2");
        source.refresh();
        assertTrue(goodCalls.get() >= 1);
    }
}
