package com.kato.pro.langchain.domain.sync;

import com.kato.pro.langchain.common.exception.BusinessException;
import com.kato.pro.langchain.config.SyncProperties;
import com.kato.pro.langchain.domain.knowledge.KnowledgeDoc;
import com.kato.pro.langchain.domain.knowledge.KnowledgeDocService;
import com.kato.pro.langchain.domain.knowledge.SourceType;
import com.kato.pro.langchain.infrastructure.persistence.SyncRunRecordMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * SyncApplicationService 单元测试。
 *
 * 关键策略（M7 已验证）：
 *   - SyncRunRecordMapper（BaseMapper）→ JDK Proxy 拦截 insert/selectById/updateById
 *   - KnowledgeDocService（非 BaseMapper）→ 手写子类只 override create()
 *   - SyncAdapter → 匿名内部 stub，自由控制 fetchSince 行为
 *
 * 覆盖：
 *   1. SUCCESS  - adapter 正常返回 2 条 record，全部 create 成功
 *   2. PARTIAL  - 2 条 record，其中 1 条 docService.create 抛异常
 *   3. FAILED   - adapter.fetchSince 抛异常
 *   4. unknownAdapter  - runOne 抛 BusinessException
 *   5. statusOverview  - 2 个 adapter 都有 lastRun
 */
class SyncApplicationServiceTest {

    private static final MapperState STATE = new MapperState();
    private SyncRunRecordMapper runMapper;
    private FakeDocService docService;
    private SyncProperties properties;
    private SyncApplicationService service;

    @BeforeEach
    void setUp() {
        runMapper = proxyMapper();
        docService = new FakeDocService();
        properties = new SyncProperties();
        properties.setEnabled(true);
        properties.setDefaultTenant(1L);
    }

    @AfterEach
    void tearDown() {
        STATE.clear();
        docService.throwOnExternalId = null;
    }

    private SyncApplicationService buildService(List<SyncAdapter> adapters) {
        return new SyncApplicationService(properties, adapters, docService, runMapper);
    }

    // ---- 1. SUCCESS ----
    @Test
    void runOne_allSuccess_recordsSuccess() {
        List<SyncRecord> recs = List.of(
                SyncRecord.builder().externalId("A").title("ta").content("ca").updatedAt(Instant.now()).build(),
                SyncRecord.builder().externalId("B").title("tb").content("cb").updatedAt(Instant.now()).build());
        SyncAdapter stub = stubAdapter("product_catalog", SourceType.SYNC_PRODUCT, recs, null);
        service = buildService(List.of(stub));

        SyncRunRecord run = service.runOne("product_catalog");

        assertEquals(SyncStatus.SUCCESS.name(), run.getStatus());
        assertEquals(2, run.getSuccessCount());
        assertEquals(0, run.getFailCount());
        assertNotNull(run.getEndTime());
        assertNull(run.getErrorMsg());
    }

    // ---- 2. PARTIAL ----
    @Test
    void runOne_partialFailure_recordsPartial() {
        List<SyncRecord> recs = List.of(
                SyncRecord.builder().externalId("A").title("ta").content("ca").updatedAt(Instant.now()).build(),
                SyncRecord.builder().externalId("BAD").title("tbad").content("cbad").updatedAt(Instant.now()).build());
        docService.throwOnExternalId = "BAD";
        SyncAdapter stub = stubAdapter("product_catalog", SourceType.SYNC_PRODUCT, recs, null);
        service = buildService(List.of(stub));

        SyncRunRecord run = service.runOne("product_catalog");

        assertEquals(SyncStatus.PARTIAL.name(), run.getStatus());
        assertEquals(1, run.getSuccessCount());
        assertEquals(1, run.getFailCount());
        assertNull(run.getErrorMsg()); // PARTIAL 状态下 errorMsg 留给整 adapter 失败用
    }

    // ---- 3. FAILED ----
    @Test
    void runOne_adapterThrows_recordsFailed() {
        SyncAdapter stub = stubAdapter("product_catalog", SourceType.SYNC_PRODUCT, List.of(),
                new RuntimeException("upstream down"));
        service = buildService(List.of(stub));

        SyncRunRecord run = service.runOne("product_catalog");

        assertEquals(SyncStatus.FAILED.name(), run.getStatus());
        assertEquals(0, run.getSuccessCount());
        assertEquals(0, run.getFailCount());
        assertEquals("upstream down", run.getErrorMsg());
    }

    // ---- 4. unknownAdapter ----
    @Test
    void runOne_unknownAdapter_throwsBusinessException() {
        SyncAdapter stub = stubAdapter("product_catalog", SourceType.SYNC_PRODUCT, List.of(), null);
        service = buildService(List.of(stub));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.runOne("nonexistent"));
        assertTrue(ex.getMessage().contains("nonexistent"));
    }

    // ---- 5. statusOverview ----
    @Test
    void statusOverview_listsAllAdaptersWithLastRun() {
        // 先跑一次，让 mapper 有数据
        SyncAdapter a = stubAdapter("product_catalog", SourceType.SYNC_PRODUCT,
                List.of(SyncRecord.builder().externalId("X").title("x").content("cx").updatedAt(Instant.now()).build()),
                null);
        SyncAdapter b = stubAdapter("policy_doc", SourceType.SYNC_POLICY, List.of(), null);
        service = buildService(List.of(a, b));
        service.runOne("product_catalog");
        service.runOne("policy_doc");

        List<Map<String, Object>> overview = service.statusOverview();
        assertEquals(2, overview.size());

        Map<String, Object> aEntry = overview.stream()
                .filter(e -> "product_catalog".equals(e.get("adapterName"))).findFirst().orElseThrow();
        assertEquals("商品库", aEntry.get("description"));
        assertEquals("SYNC_PRODUCT", aEntry.get("sourceType"));
        assertNotNull(aEntry.get("lastRun"));

        Map<String, Object> bEntry = overview.stream()
                .filter(e -> "policy_doc".equals(e.get("adapterName"))).findFirst().orElseThrow();
        assertEquals("政策库", bEntry.get("description"));
        assertNotNull(bEntry.get("lastRun"));
    }

    // ---- helpers ----

    private static SyncAdapter stubAdapter(String name, SourceType type, List<SyncRecord> records, RuntimeException toThrow) {
        return new SyncAdapter() {
            @Override public String name() { return name; }
            @Override public SourceType sourceType() { return type; }
            @Override public List<SyncRecord> fetchSince(Instant lastSyncTime) {
                if (toThrow != null) throw toThrow;
                return records;
            }
        };
    }

    // ---- JDK Proxy mapper stub（M7 验证模式）----

    static final class MapperState {
        final Map<Long, SyncRunRecord> store = new HashMap<>();
        final AtomicLong seq = new AtomicLong(0);
        void clear() { store.clear(); seq.set(0); }
    }

    @SuppressWarnings("unchecked")
    private static SyncRunRecordMapper proxyMapper() {
        InvocationHandler handler = (proxy, method, args) -> {
            String n = method.getName();
            switch (n) {
                case "insert": {
                    SyncRunRecord r = (SyncRunRecord) args[0];
                    long id = STATE.seq.incrementAndGet();
                    r.setId(id);
                    STATE.store.put(id, r);
                    return 1;
                }
                case "selectById": {
                    java.io.Serializable sid = (java.io.Serializable) args[0];
                    return STATE.store.get(((Number) sid).longValue());
                }
                case "updateById": {
                    SyncRunRecord r = (SyncRunRecord) args[0];
                    STATE.store.put(r.getId(), r);
                    return 1;
                }
                case "findRecentByAdapter": {
                    String adapter = (String) args[0];
                    int limit = (int) args[1];
                    List<SyncRunRecord> all = new ArrayList<>();
                    for (SyncRunRecord r : STATE.store.values()) {
                        if (adapter == null || adapter.equalsIgnoreCase(r.getAdapterName())) {
                            all.add(r);
                        }
                    }
                    all.sort((x, y) -> y.getStartTime().compareTo(x.getStartTime()));
                    if (all.size() > limit) return all.subList(0, limit);
                    return all;
                }
                case "selectCount": return 0L;
                case "selectList": return new ArrayList<>();
                case "selectMaps": return new ArrayList<>();
                case "selectObjs": return new ArrayList<>();
                default:
                    Class<?> rt = method.getReturnType();
                    if (rt == int.class) return 0;
                    if (rt == long.class) return 0L;
                    if (rt == boolean.class) return false;
                    if (rt == void.class) return null;
                    return null;
            }
        };
        return (SyncRunRecordMapper) Proxy.newProxyInstance(
                SyncRunRecordMapper.class.getClassLoader(),
                new Class<?>[]{SyncRunRecordMapper.class},
                handler);
    }

    /** 手写 stub：只 override create()，其余方法继承父类但不会被调用 */
    static class FakeDocService extends KnowledgeDocService {
        String throwOnExternalId;
        long seq = 0;
        FakeDocService() { super(null, null, null, null); }
        @Override
        public KnowledgeDoc create(String title, String fileName, String contentType,
                                    byte[] content, SourceType sourceType, String sourceId) {
            if (throwOnExternalId != null && throwOnExternalId.equals(sourceId)) {
                throw new RuntimeException("simulated create failure for " + sourceId);
            }
            KnowledgeDoc d = new KnowledgeDoc();
            d.setId(++seq);
            d.setTitle(title);
            d.setFileName(fileName);
            d.setSourceId(sourceId);
            d.setSourceType(sourceType);
            return d;
        }
    }
}
