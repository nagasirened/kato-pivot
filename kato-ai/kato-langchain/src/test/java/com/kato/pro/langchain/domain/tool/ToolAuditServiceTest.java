package com.kato.pro.langchain.domain.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kato.pro.langchain.annotation.ToolDef;
import com.kato.pro.langchain.common.tenant.TenantContext;
import com.kato.pro.langchain.common.tenant.TenantInfo;
import com.kato.pro.langchain.infrastructure.persistence.ToolCallAuditMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * ToolAuditService 测试。
 *
 * MyBatis-Plus BaseMapper 有 19 个 abstract 方法（含 8 个带 ResultHandler&lt;E&gt; 泛型擦除），
 * sandbox JDK21 下 Mockito-inline + byte-buddy 不可用，手写 stub 19 个方法 boilerplate 灾难。
 * 这里用 JDK Proxy 拦截 3 个 service 真实调用（selectById/insert/updateById），
 * 其它方法返回默认零值（null/0/empty）。
 */
class ToolAuditServiceTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private ToolCallAuditMapper auditMapper;
    private ToolRegistry registry;
    private ToolInvoker invoker;
    private ToolProperties properties;
    private ToolAuditService service;

    @BeforeEach
    void setUp() {
        TenantContext.set(TenantInfo.of(1L, 100L));
        registry = new ToolRegistry();
        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
                ctx.register(WriteTool.class);
        ctx.refresh();
        registry.setApplicationContext(ctx);
        invoker = new StubInvoker(ToolResult.ok(Map.of("ok", true)));
        properties = new ToolProperties();
        properties.setEnabled(true);
        properties.setDefaultTimeoutMs(1000);
        auditMapper = proxyMapper();
        service = new ToolAuditService(auditMapper, registry, invoker, properties, mapper);
    }

    @AfterEach
    void tearDown() { TenantContext.clear(); }

    @Test
    void enqueue_insertsPendingAudit() {
        ToolContext ctx = ToolContext.builder()
                .tenantId(1L).userId(100L).traceId("t1").channel("user").build();
        Long id = service.enqueue("w", "WRITE", mapper.createObjectNode(), ctx);
        assertNotNull(id);
        ToolCallAudit a = state.store.get(id);
        assertEquals("PENDING", a.getStatus());
    }

    @Test
    void executeApproved_changesStatusToExecuted() {
        ToolCallAudit a = new ToolCallAudit();
        a.setId(1L); a.setTenantId(1L); a.setToolName("w"); a.setToolType("WRITE");
        a.setArgsJson("{}"); a.setRequesterId(100L); a.setChannel("user");
        a.setStatus(AuditStatus.PENDING.name());
        state.store.put(1L, a);
        ToolResult r = service.executeApproved(1L);
        assertTrue(r.isSuccess());
        assertEquals("EXECUTED", r.getData().get("status"));
        assertEquals(AuditStatus.EXECUTED.name(), state.store.get(1L).getStatus());
    }

    @Test
    void reject_changesStatusToRejected() {
        ToolCallAudit a = new ToolCallAudit();
        a.setId(1L); a.setTenantId(1L); a.setToolName("w");
        a.setStatus(AuditStatus.PENDING.name());
        state.store.put(1L, a);
        ToolResult r = service.reject(1L, 200L);
        assertTrue(r.isSuccess());
        assertEquals("REJECTED", r.getData().get("status"));
        assertEquals(AuditStatus.REJECTED.name(), state.store.get(1L).getStatus());
    }

    @Test
    void executeApproved_wrongStatus_throws() {
        ToolCallAudit a = new ToolCallAudit();
        a.setId(1L); a.setStatus(AuditStatus.EXECUTED.name());
        state.store.put(1L, a);
        assertThrows(com.kato.pro.langchain.common.exception.BusinessException.class,
                () -> service.executeApproved(1L));
    }

    // ---- JDK Proxy mapper stub ----
    static final class MapperState {
        final Map<Long, ToolCallAudit> store = new HashMap<>();
        final AtomicLong seq = new AtomicLong(0);
    }
    static final MapperState state = new MapperState();

    @SuppressWarnings("unchecked")
    private static ToolCallAuditMapper proxyMapper() {
        InvocationHandler handler = (proxy, method, args) -> {
            String name = method.getName();
            switch (name) {
                case "insert": {
                    ToolCallAudit a = (ToolCallAudit) args[0];
                    long id = state.seq.incrementAndGet();
                    a.setId(id);
                    state.store.put(id, a);
                    return 1;
                }
                case "selectById": {
                    java.io.Serializable sid = (java.io.Serializable) args[0];
                    return state.store.get(((Number) sid).longValue());
                }
                case "updateById": {
                    ToolCallAudit a = (ToolCallAudit) args[0];
                    state.store.put(a.getId(), a);
                    return 1;
                }
                case "deleteById":
                    state.store.remove(((Number) args[0]).longValue());
                    return 1;
                case "selectCount": return 0L;
                case "selectList": return java.util.List.of();
                case "selectMaps": return java.util.List.of();
                case "selectObjs": return java.util.List.of();
                default:
                    Class<?> rt = method.getReturnType();
                    if (rt == int.class) return 0;
                    if (rt == long.class) return 0L;
                    if (rt == boolean.class) return false;
                    if (rt == void.class) return null;
                    return null;
            }
        };
        return (ToolCallAuditMapper) Proxy.newProxyInstance(
                ToolCallAuditMapper.class.getClassLoader(),
                new Class<?>[]{ToolCallAuditMapper.class},
                handler);
    }

    static class StubInvoker implements ToolInvoker {
        private final ToolResult result;
        StubInvoker(ToolResult result) { this.result = result; }
        @Override public ToolResult invoke(Tool tool, JsonNode args, ToolContext ctx, long timeoutMs) {
            return result;
        }
    }

    @org.springframework.stereotype.Component
    @ToolDef(name = "w", type = ToolType.WRITE)
    static class WriteTool implements Tool {
        public String name() { return "w"; }
        public ToolResult execute(JsonNode args, ToolContext ctx) { return ToolResult.ok(Map.of("ok", true)); }
    }
}
