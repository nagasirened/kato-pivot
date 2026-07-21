package com.kato.pro.langchain.config.source;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * ConfigSnapshot record 行为测试。
 */
class ConfigSnapshotTest {

    @Test
    void empty_returnsEmptyValues() {
        ConfigSnapshot s = ConfigSnapshot.empty();
        assertNotNull(s);
        assertTrue(s.values().isEmpty());
        assertEquals("empty", s.source());
        assertEquals(0L, s.version());
    }

    @Test
    void get_returnsValueOrNull() {
        ConfigSnapshot s = new ConfigSnapshot(Map.of("kato.audit.path", "/tmp/a.json"), "local", 1L, 0L);
        assertEquals("/tmp/a.json", s.get("kato.audit.path"));
        assertNull(s.get("missing"));
    }

    @Test
    void getOrDefault_returnsDefaultWhenMissing() {
        ConfigSnapshot s = new ConfigSnapshot(Map.of("kato.x", "1"), "local", 1L, 0L);
        assertEquals("default", s.getOrDefault("missing", "default"));
        assertEquals("1", s.getOrDefault("kato.x", "default"));
    }

    @Test
    void has_checksKeyExistence() {
        ConfigSnapshot s = new ConfigSnapshot(Map.of("kato.x", "1"), "local", 1L, 0L);
        assertTrue(s.has("kato.x"));
        assertFalse(s.has("missing"));
    }

    @Test
    void values_isImmutable() {
        Map<String, String> backing = new java.util.HashMap<>(Map.of("a", "b"));
        ConfigSnapshot s = new ConfigSnapshot(backing, "local", 1L, 0L);
        assertThrows(UnsupportedOperationException.class, () -> s.values().put("c", "d"));
    }

    private static void assertThrows(Class<? extends Throwable> expected, Runnable r) {
        try {
            r.run();
        } catch (Throwable t) {
            if (expected.isInstance(t)) return;
            throw new AssertionError("Expected " + expected + " but got " + t.getClass(), t);
        }
        throw new AssertionError("Expected " + expected + " but no exception thrown");
    }
}
