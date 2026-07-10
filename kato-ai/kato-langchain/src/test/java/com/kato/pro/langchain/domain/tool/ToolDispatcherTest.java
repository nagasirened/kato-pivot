package com.kato.pro.langchain.domain.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kato.pro.langchain.annotation.ToolDef;
import com.kato.pro.langchain.common.exception.BusinessException;
import com.kato.pro.langchain.common.tenant.TenantContext;
import com.kato.pro.langchain.common.tenant.TenantInfo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * ToolDispatcher 测试 — 不使用 Mockito（sandbox JDK21 下 byte-buddy 不可用），
 * 全部用匿名内部类手写 stub。
 */
class ToolDispatcherTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private ToolRegistry registry;
    private ToolInvoker invoker;
    private TenantToolConfigService configService;
    private ToolAuditService auditService;
    private ToolDispatcher dispatcher;

    @BeforeEach
    void setUp() {
        TenantContext.set(TenantInfo.of(1L, 100L));
        registry = new ToolRegistry();
        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
                ctx.register(ReadTool.class, WriteTool.class);
        ctx.refresh();
        registry.setApplicationContext(ctx);
        invoker = new StubInvoker(ToolResult.ok(Map.of("k", "v")));
        configService = new StubConfigService(true);
        auditService = new StubAuditService(42L);

        ToolProperties props = new ToolProperties();
        props.setEnabled(true);
        dispatcher = new ToolDispatcher(props, registry, invoker, configService, auditService);
    }

    @AfterEach
    void tearDown() { TenantContext.clear(); }

    @Test
    void disabled_returnsFail() {
        ToolProperties props = new ToolProperties();
        props.setEnabled(false);
        ToolDispatcher d = new ToolDispatcher(props, registry, invoker, configService, auditService);
        ToolResult r = d.dispatch("r", mapper.createObjectNode());
        assertTrue(r.getError().contains("disabled"));
    }

    @Test
    void readTool_invokesInvoker() {
        ToolResult r = dispatcher.dispatch("r", mapper.createObjectNode());
        assertTrue(r.isSuccess());
        assertEquals("v", r.getData().get("k"));
    }

    @Test
    void writeTool_enqueueOnly_noInvoke() {
        ToolResult r = dispatcher.dispatch("w", mapper.createObjectNode());
        assertTrue(r.isSuccess());
        assertEquals(42L, r.getData().get("auditId"));
        assertEquals("PENDING", r.getData().get("status"));
    }

    @Test
    void toolDisabledByTenant_returnsFail() {
        ToolDispatcher d = new ToolDispatcher(new ToolProperties(),
                registry, invoker, new StubConfigService(false), auditService);
        ToolResult r = d.dispatch("r", mapper.createObjectNode());
        assertTrue(r.getError().contains("disabled for tenant"));
    }

    @Test
    void unknownTool_throws() {
        assertThrows(BusinessException.class,
                () -> dispatcher.dispatch("nope", mapper.createObjectNode()));
    }

    // ---- 手写 stub ----
    static class StubInvoker implements ToolInvoker {
        private final ToolResult result;
        StubInvoker(ToolResult result) { this.result = result; }
        @Override public ToolResult invoke(Tool tool, JsonNode args, ToolContext ctx, long timeoutMs) {
            return result;
        }
    }

    static class StubConfigService extends TenantToolConfigService {
        private final boolean enabled;
        StubConfigService(boolean enabled) {
            super(null, new ToolRegistry());
            this.enabled = enabled;
        }
        @Override public boolean isEnabled(Long tenantId, String toolName) { return enabled; }
    }

    static class StubAuditService extends ToolAuditService {
        private final long auditId;
        StubAuditService(long auditId) {
            super(null, new ToolRegistry(), null, new ToolProperties(), new ObjectMapper());
            this.auditId = auditId;
        }
        @Override public Long enqueue(String toolName, String toolType, JsonNode args, ToolContext ctx) {
            return auditId;
        }
        @Override public void recordReadInvocation(String toolName, String toolType, JsonNode args,
                                                   ToolResult result, ToolContext ctx) {
            // no-op: 不调 mapper，模拟读类审计被吞掉
        }
    }

    @org.springframework.stereotype.Component
    @ToolDef(name = "r", type = ToolType.READ)
    static class ReadTool implements Tool {
        public String name() { return "r"; }
        public ToolResult execute(JsonNode args, ToolContext ctx) {
            return ToolResult.ok(Map.of("userId", ctx.getUserId()));
        }
    }
    @org.springframework.stereotype.Component
    @ToolDef(name = "w", type = ToolType.WRITE)
    static class WriteTool implements Tool {
        public String name() { return "w"; }
        public ToolResult execute(JsonNode args, ToolContext ctx) { return ToolResult.ok(Map.of()); }
    }
}
