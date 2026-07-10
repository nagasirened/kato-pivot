package com.kato.pro.langchain.domain.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.kato.pro.langchain.annotation.ToolDef;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ToolRegistryTest {

    private static ApplicationContext freshCtx(Class<?>... classes) {
        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
        if (classes.length > 0) ctx.register(classes);
        ctx.refresh();
        return ctx;
    }

    @Test
    void registersAllAnnotatedTools() {
        ApplicationContext ctx = freshCtx(OkTool.class);
        ToolRegistry r = new ToolRegistry();
        r.setApplicationContext(ctx);
        assertEquals(1, r.listAll().size());
        assertEquals("ok", r.require("ok").name());
    }

    @Test
    void duplicateName_fails() {
        ApplicationContext ctx = freshCtx(Dup1.class, Dup2.class);
        ToolRegistry r = new ToolRegistry();
        assertThrows(IllegalStateException.class, () -> r.setApplicationContext(ctx));
    }

    @Test
    void require_missing_throws() {
        ToolRegistry r = new ToolRegistry();
        r.setApplicationContext(freshCtx());
        assertThrows(com.kato.pro.langchain.common.exception.BusinessException.class,
                () -> r.require("nope"));
    }

    @Test
    void get_returnsEmpty_whenMissing() {
        ToolRegistry r = new ToolRegistry();
        r.setApplicationContext(freshCtx());
        Optional<Tool> t = r.get("nope");
        assertTrue(t.isEmpty());
    }

    @org.springframework.stereotype.Component
    @ToolDef(name = "ok", type = ToolType.READ, description = "ok")
    static class OkTool implements Tool {
        public String name() { return "ok"; }
        public ToolResult execute(JsonNode args, ToolContext ctx) { return ToolResult.ok(Map.of()); }
    }

    @org.springframework.stereotype.Component
    @ToolDef(name = "dup")
    static class Dup1 implements Tool {
        public String name() { return "dup"; }
        public ToolResult execute(JsonNode a, ToolContext c) { return ToolResult.ok(Map.of()); }
    }

    @org.springframework.stereotype.Component
    @ToolDef(name = "dup")
    static class Dup2 implements Tool {
        public String name() { return "dup"; }
        public ToolResult execute(JsonNode a, ToolContext c) { return ToolResult.ok(Map.of()); }
    }
}
