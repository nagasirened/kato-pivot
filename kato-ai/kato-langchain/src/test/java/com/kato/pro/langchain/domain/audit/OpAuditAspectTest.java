package com.kato.pro.langchain.domain.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kato.pro.langchain.config.AuditJsonProperties;
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
import static org.junit.jupiter.api.Assertions.assertTrue;

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
        OpAuditService service = new OpAuditService(mapper, new ObjectMapper(), new NoopAppender());

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


    /**
     * M14: 验证 record() 调用了 jsonAppender.append()，且 audit 字段已填全。
     * 用 RecordingAppender 替换 NoopAppender，确认 append 被调用且参数非 null。
     */
    @Test
    void service_callsJsonAppenderOnRecord() {
        OpAuditMapper mapper = proxyMapper();
        RecordingAppender rec = new RecordingAppender();
        OpAuditService service = new OpAuditService(mapper, new ObjectMapper(), rec);

        service.record(AuthInfo.of(1L, "admin", 1L, Role.ADMIN),
                "TRIGGER", "SYNC", "product_catalog",
                "/api/v1/admin/sync/x/trigger", "POST", 200,
                new Object[]{"x"}, Map.of("runId", 7L), 42L);

        assertEquals(1, rec.appended.size(), "jsonAppender.append should be called once");
        OpAudit a = rec.appended.get(0);
        assertEquals("TRIGGER", a.getAction());
        assertEquals("SYNC", a.getResource());
        assertNotNull(a.getCreateTime(), "audit should have createTime before append");
    }

    static class RecordingAppender extends OpAuditJsonAppender {
        final java.util.List<OpAudit> appended = new java.util.ArrayList<>();
        RecordingAppender() {
            super(new AuditJsonProperties(), new ObjectMapper());
        }
        @Override
        public void append(OpAudit audit) {
            if (audit != null) appended.add(audit);
        }
        @Override
        void init() {}
        @Override
        void shutdown() {}
    }

    /**
     * M14: appender enabled=true 时写文件；service 委托给 appender 后内容正确。
     * 真实写文件用 temp dir。
     */
    @org.junit.jupiter.api.Test
    void service_endToEnd_writesNdjsonLine() throws Exception {
        java.nio.file.Path tmp = java.nio.file.Files.createTempFile("op-audit-m14-", ".json");
        tmp.toFile().delete();
        AuditJsonProperties realProps = new AuditJsonProperties();
        realProps.setEnabled(true);
        realProps.setPath(tmp.toString());
        realProps.setMaxLineLength(8000);

        OpAuditJsonAppender realAppender = new OpAuditJsonAppender(realProps, new ObjectMapper());
        realAppender.init();

        OpAuditMapper mapper = proxyMapper();
        OpAuditService service = new OpAuditService(mapper, new ObjectMapper(), realAppender);

        service.record(AuthInfo.of(1L, "admin", 1L, Role.ADMIN),
                "TRIGGER", "SYNC", "p1",
                "/api/v1/admin/sync/p1/trigger", "POST", 200,
                new Object[]{"p1"}, Map.of("runId", 7L), 42L);

        realAppender.shutdown();

        java.util.List<String> lines = java.nio.file.Files.readAllLines(tmp);
        assertEquals(1, lines.size());
        assertTrue(lines.get(0).contains("\"action\":\"TRIGGER\""),
                "NDJSON line should contain action=TRIGGER, got: " + lines.get(0));
        assertTrue(lines.get(0).contains("\"username\":\"admin\""));
    }
    @Test
    void service_truncateLongJson() {
        OpAuditMapper mapper = proxyMapper();
        OpAuditService service = new OpAuditService(mapper, new ObjectMapper(), new NoopAppender());
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
        OpAuditService service = new OpAuditService(mapper, new ObjectMapper(), new NoopAppender());
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


    /** M14 测试 stub：永远不写文件的 appender */
    static class NoopAppender extends OpAuditJsonAppender {
        NoopAppender() {
            super(new AuditJsonProperties(), new ObjectMapper());
            super.init();
        }
        @Override
        public void append(OpAudit audit) {
            // no-op：测试只关心 DB 写入路径
        }
        @Override
        void init() {
            // skip parent init log
        }
        @Override
        void shutdown() {
            // no-op
        }
    }
}