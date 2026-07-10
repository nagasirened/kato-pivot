package com.kato.pro.langchain.common.trace;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * TraceContext 单元测试。
 *
 * v1 走 SLF4J MDC：同线程 set/clear/current 行为符合预期；
 * wrap/runWith 用于非 web 入口（@Scheduled / MQ consumer）。
 */
class TraceContextTest {

    @AfterEach
    void tearDown() { TraceContext.clear(); }

    @Test
    void current_unset_returnsNull() {
        TraceContext.clear();
        assertNull(TraceContext.current());
    }

    @Test
    void set_thenCurrent_returnsValue() {
        TraceContext.set("abc-123");
        assertEquals("abc-123", TraceContext.current());
    }

    @Test
    void clear_removesValue() {
        TraceContext.set("tmp");
        TraceContext.clear();
        assertNull(TraceContext.current());
    }

    @Test
    void generate_returnsNonBlankAndSets() {
        String t = TraceContext.generate();
        assertNotNull(t);
        assertEquals(t, TraceContext.current());
        assertEquals(32, t.length()); // UUID 去 dash
    }

    @Test
    void wrap_setsAndRestores() {
        TraceContext.set("outer");
        String result = TraceContext.wrap("inner", () -> {
            assertEquals("inner", TraceContext.current());
            return "ok";
        });
        assertEquals("ok", result);
        // wrap 结束后应恢复 outer
        assertEquals("outer", TraceContext.current());
    }

    @Test
    void wrap_whenNoPrev_clears() {
        TraceContext.clear();
        TraceContext.wrap("inside", () -> {
            assertEquals("inside", TraceContext.current());
            return null;
        });
        assertNull(TraceContext.current());
    }

    @Test
    void runWith_executesAndClears() {
        TraceContext.set("prev");
        TraceContext.runWith("task-trace", () -> {
            assertEquals("task-trace", TraceContext.current());
        });
        assertEquals("prev", TraceContext.current());
    }
}
