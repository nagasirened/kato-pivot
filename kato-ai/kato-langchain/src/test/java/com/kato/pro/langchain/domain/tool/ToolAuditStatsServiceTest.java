package com.kato.pro.langchain.domain.tool;

import com.kato.pro.langchain.api.tool.dto.AuditStatsVO;
import com.kato.pro.langchain.common.tenant.TenantContext;
import com.kato.pro.langchain.common.tenant.TenantInfo;
import com.kato.pro.langchain.infrastructure.persistence.ToolCallAuditMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * ToolAuditStatsService 单元测试。
 *
 * mapper 走 JDK Proxy + 静态 store 模式（M7 验证过）；
 * 覆盖 3 个维度聚合：status / tool / day。
 */
class ToolAuditStatsServiceTest {

    private static final MapperState STATE = new MapperState();
    private ToolCallAuditMapper mapper;
    private ToolAuditStatsService service;

    @BeforeEach
    void setUp() {
        TenantContext.set(TenantInfo.of(1L, 100L));
        STATE.clear();
        // 预填 5 条 audit，覆盖 3 个 status / 2 个 toolName / 2 个 day
        seed(1L, "w",  "PENDING",  todayMinus(1));
        seed(2L, "w",  "EXECUTED", todayMinus(1));
        seed(3L, "w",  "EXECUTED", todayMinus(0));
        seed(4L, "r",  "REJECTED", todayMinus(0));
        seed(5L, "r",  "EXECUTED", todayMinus(0));
        mapper = proxyMapper();
        service = new ToolAuditStatsService(mapper);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
        STATE.clear();
    }

    @Test
    void stats_aggregatesTotalAndByStatus() {
        AuditStatsVO vo = service.stats(7);
        assertNotNull(vo);
        assertEquals(5L, vo.getTotal());
        assertEquals(1L, vo.getByStatus().get("PENDING"));
        assertEquals(3L, vo.getByStatus().get("EXECUTED"));
        assertEquals(1L, vo.getByStatus().get("REJECTED"));
    }

    @Test
    void stats_aggregatesByTool() {
        AuditStatsVO vo = service.stats(7);
        assertEquals(3L, vo.getByTool().get("w"));
        assertEquals(2L, vo.getByTool().get("r"));
    }

    @Test
    void stats_aggregatesByDay_includesOnlySinceDay() {
        AuditStatsVO vo = service.stats(7);
        Map<String, Long> byDay = vo.getByDay();
        assertNotNull(byDay);
        // todayMinus(1) 1 条 + todayMinus(0) 3 条
        long todayCount = byDay.getOrDefault(LocalDate.now().toString(), 0L);
        long yesterdayCount = byDay.getOrDefault(LocalDate.now().minusDays(1).toString(), 0L);
        assertEquals(3L, todayCount);
        assertEquals(2L, yesterdayCount);
    }

    @Test
    void stats_emptyStore_returnsZeros() {
        STATE.clear();
        AuditStatsVO vo = service.stats(7);
        assertEquals(0L, vo.getTotal());
        assertTrue(vo.getByStatus().isEmpty());
        assertTrue(vo.getByTool().isEmpty());
        assertTrue(vo.getByDay().isEmpty());
    }

    // ---- helpers ----

    private static String todayMinus(int days) {
        return LocalDate.now().minusDays(days).toString();
    }

    private static void seed(long id, String tool, String status, String day) {
        ToolCallAudit a = new ToolCallAudit();
        a.setId(id);
        a.setToolName(tool);
        a.setStatus(status);
        a.setCreateTime(java.time.LocalDateTime.parse(day + "T10:00:00"));
        STATE.store.put(id, a);
    }

    // ---- JDK Proxy mapper stub ----

    static final class MapperState {
        final Map<Long, ToolCallAudit> store = new HashMap<>();
        final AtomicLong seq = new AtomicLong(0);
        void clear() { store.clear(); seq.set(0); }
    }

    @SuppressWarnings("unchecked")
    private static ToolCallAuditMapper proxyMapper() {
        InvocationHandler handler = (proxy, method, args) -> {
            String n = method.getName();
            switch (n) {
                case "selectById":
                    return STATE.store.get(((Number) args[0]).longValue());
                case "selectList":
                case "selectMaps": {
                    // 简单 selectList：返回所有
                    return new ArrayList<>(STATE.store.values());
                }
                case "countByStatus": {
                    Map<String, Long> map = new HashMap<>();
                    for (ToolCallAudit a : STATE.store.values()) {
                        map.merge(a.getStatus(), 1L, Long::sum);
                    }
                    List<Map<String, Object>> rows = new ArrayList<>();
                    map.forEach((k, v) -> {
                        Map<String, Object> r = new HashMap<>();
                        r.put("status", k);
                        r.put("cnt", v);
                        rows.add(r);
                    });
                    return rows;
                }
                case "countByTool": {
                    Map<String, Long> map = new HashMap<>();
                    for (ToolCallAudit a : STATE.store.values()) {
                        map.merge(a.getToolName(), 1L, Long::sum);
                    }
                    List<Map<String, Object>> rows = new ArrayList<>();
                    map.forEach((k, v) -> {
                        Map<String, Object> r = new HashMap<>();
                        r.put("tool_name", k);
                        r.put("cnt", v);
                        rows.add(r);
                    });
                    return rows;
                }
                case "countByDay": {
                    LocalDate since = (LocalDate) args[0];
                    Map<String, Long> map = new HashMap<>();
                    for (ToolCallAudit a : STATE.store.values()) {
                        LocalDate d = a.getCreateTime().toLocalDate();
                        if (d.isBefore(since)) continue;
                        map.merge(d.toString(), 1L, Long::sum);
                    }
                    List<Map<String, Object>> rows = new ArrayList<>();
                    map.forEach((k, v) -> {
                        Map<String, Object> r = new HashMap<>();
                        r.put("day", k);
                        r.put("cnt", v);
                        rows.add(r);
                    });
                    rows.sort((x, y) -> ((String) x.get("day")).compareTo((String) y.get("day")));
                    return rows;
                }
                case "selectCount": return (long) STATE.store.size();
                case "insert": return 1;
                case "updateById": return 1;
                case "deleteById": return 1;
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
        return (ToolCallAuditMapper) Proxy.newProxyInstance(
                ToolCallAuditMapper.class.getClassLoader(),
                new Class<?>[]{ToolCallAuditMapper.class},
                handler);
    }
}
