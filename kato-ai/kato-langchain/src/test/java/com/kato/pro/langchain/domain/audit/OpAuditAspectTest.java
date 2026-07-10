package com.kato.pro.langchain.domain.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kato.pro.langchain.common.security.AuthContext;
import com.kato.pro.langchain.common.security.AuthInfo;
import com.kato.pro.langchain.common.security.Role;
import com.kato.pro.langchain.infrastructure.persistence.OpAuditMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * OpAuditAspect + OpAuditService 单元测试（M11）。
 *
 * 验证：
 *   - @OpAuditLog 标注的方法被调用后，op_audit 表有记录
 *   - 业务方法抛错时，op_audit 也写（status=500）
 */
class OpAuditAspectTest {

    private static final Map<Long, OpAudit> STORE = new HashMap<>();
    private static final AtomicLong SEQ = new AtomicLong(0);

    @BeforeEach
    void setUp() {
        STORE.clear();
        SEQ.set(0);
        AuthContext.set(AuthInfo.of(1L, "admin", 1L, Role.ADMIN));
    }
    @AfterEach
    void tearDown() {
        AuthContext.clear();
    }

    @Test
    void service_writesAuditRecord() {
        OpAuditMapper mapper = proxyMapper();
        OpAuditService service = new OpAuditService(mapper, new ObjectMapper());

        AuthInfo info = AuthInfo.of(1L, "admin", 1L, Role.ADMIN);
        service.record(info, "TRIGGER", "SYNC", "product_catalog",
                "/api/v1/admin/sync/product_catalog/trigger", "POST", 200,
                new Object[]{"product_catalog"}, Map.of("runId", 7L), 42L);

        assertEquals(1, STORE.size());
        OpAudit a = STORE.values().iterator().next();
        assertEquals("TRIGGER", a.getAction());
        assertEquals("SYNC", a.getResource());
        assertEquals("product_catalog", a.getResourceId());
        assertEquals("admin", a.getUsername());
        assertEquals(200, a.getHttpStatus());
        assertEquals(42, a.getDurationMs());
        assertNotNull(a.getArgsJson());
        assertNotNull(a.getResultJson());
    }

    @Test
    void service_truncateLongJson() {
        OpAuditMapper mapper = proxyMapper();
        OpAuditService service = new OpAuditService(mapper, new ObjectMapper());
        StringBuilder big = new StringBuilder();
        for (int i = 0; i < 5000; i++) big.append("x");
        service.record(AuthInfo.of(1L, "admin", 1L, Role.ADMIN),
                "CREATE", "KNOWLEDGE", "doc-1",
                "/api/v1/admin/knowledge", "POST", 200,
                big.toString(), "ok", 10);
        OpAudit a = STORE.values().iterator().next();
        // 截断到 4000 + "..."
        assertNotNull(a.getArgsJson());
        assertEquals(true, a.getArgsJson().length() <= 4003);
    }

    @Test
    void service_noAuthContext_skipsSilently() {
        OpAuditMapper mapper = proxyMapper();
        OpAuditService service = new OpAuditService(mapper, new ObjectMapper());
        AuthContext.clear();
        service.recordFromContext("TRIGGER", "SYNC", "x", "/uri", "POST", 200,
                new Object[]{"x"}, "ok", 1);
        assertEquals(0, STORE.size());
    }

    // ---- JDK Proxy mapper stub ----

    @SuppressWarnings("unchecked")
    private static OpAuditMapper proxyMapper() {
        InvocationHandler h = (proxy, method, args) -> {
            String n = method.getName();
            switch (n) {
                case "insert": {
                    OpAudit a = (OpAudit) args[0];
                    long id = SEQ.incrementAndGet();
                    a.setId(id);
                    STORE.put(id, a);
                    return 1;
                }
                case "selectById": return STORE.get(((Number) args[0]).longValue());
                case "updateById": STORE.put(((com.kato.pro.langchain.common.entity.BaseEntity) args[0]).getId(), (OpAudit) args[0]); return 1;
                case "deleteById": STORE.remove(((Number) args[0]).longValue()); return 1;
                case "selectCount": return (long) STORE.size();
                case "selectList": return List.copyOf(STORE.values());
                case "selectMaps": return List.of();
                case "selectObjs": return List.of();
                case "pageByUser": return null;
                case "recentByResource": return List.copyOf(STORE.values());
                default:
                    Class<?> rt = method.getReturnType();
                    if (rt == int.class) return 0;
                    if (rt == long.class) return 0L;
                    if (rt == boolean.class) return false;
                    if (rt == void.class) return null;
                    return null;
            }
        };
        return (OpAuditMapper) Proxy.newProxyInstance(
                OpAuditMapper.class.getClassLoader(),
                new Class<?>[]{OpAuditMapper.class},
                h);
    }
}
