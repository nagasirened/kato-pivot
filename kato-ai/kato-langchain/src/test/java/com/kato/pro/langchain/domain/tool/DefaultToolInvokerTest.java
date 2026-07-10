package com.kato.pro.langchain.domain.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

class DefaultToolInvokerTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private final ToolContext ctx = ToolContext.builder()
            .tenantId(1L).userId(100L).traceId("t1").channel("user").build();

    @Test
    void success_returnsResult_andRecordsSuccess() {
        SimpleCircuitBreaker cb = new SimpleCircuitBreaker(5, 1000);
        DefaultToolInvoker inv = new DefaultToolInvoker(cb);
        Tool tool = stub("ok", args -> ToolResult.ok(Map.of("k", "v")));
        ToolResult r = inv.invoke(tool, mapper.createObjectNode(), ctx, 1000);
        assertTrue(r.isSuccess());
        assertEquals("v", r.getData().get("k"));
    }

    @Test
    void failure_returnsFail_andRecordsFailure() {
        SimpleCircuitBreaker cb = new SimpleCircuitBreaker(5, 1000);
        DefaultToolInvoker inv = new DefaultToolInvoker(cb);
        Tool tool = stub("fail", args -> ToolResult.fail("boom"));
        ToolResult r = inv.invoke(tool, mapper.createObjectNode(), ctx, 1000);
        assertFalse(r.isSuccess());
        assertEquals("boom", r.getError());
    }

    @Test
    void timeout_returnsFailAfterDeadline() {
        SimpleCircuitBreaker cb = new SimpleCircuitBreaker(5, 1000);
        DefaultToolInvoker inv = new DefaultToolInvoker(cb);
        Tool tool = stub("slow", args -> {
            try { Thread.sleep(2000); } catch (InterruptedException ignored) {}
            return ToolResult.ok(Map.of());
        });
        ToolResult r = inv.invoke(tool, mapper.createObjectNode(), ctx, 100);
        assertFalse(r.isSuccess());
        assertTrue(r.getError().contains("timeout"));
    }

    @Test
    void exception_insideTool_returnsFail() {
        SimpleCircuitBreaker cb = new SimpleCircuitBreaker(5, 1000);
        DefaultToolInvoker inv = new DefaultToolInvoker(cb);
        Tool tool = stub("throw", args -> { throw new RuntimeException("nope"); });
        ToolResult r = inv.invoke(tool, mapper.createObjectNode(), ctx, 1000);
        assertFalse(r.isSuccess());
        assertTrue(r.getError().contains("nope"));
    }

    @Test
    void circuitOpen_rejectsImmediately() {
        SimpleCircuitBreaker cb = new SimpleCircuitBreaker(2, 60_000);
        DefaultToolInvoker inv = new DefaultToolInvoker(cb);
        cb.recordFailure("a");
        cb.recordFailure("a");
        Tool tool = stub("a", args -> ToolResult.ok(Map.of()));
        ToolResult r = inv.invoke(tool, mapper.createObjectNode(), ctx, 1000);
        assertFalse(r.isSuccess());
        assertTrue(r.getError().contains("circuit open"));
    }

    private Tool stub(String name, java.util.function.Function<JsonNode, ToolResult> exec) {
        return new Tool() {
            @Override public String name() { return name; }
            @Override public ToolResult execute(JsonNode args, ToolContext c) { return exec.apply(args); }
        };
    }
}
