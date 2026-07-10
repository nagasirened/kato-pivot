package com.kato.pro.langchain.domain.tool;

import com.kato.pro.langchain.api.tool.dto.AuditStatsVO;
import com.kato.pro.langchain.common.tenant.TenantContext;
import com.kato.pro.langchain.infrastructure.persistence.ToolCallAuditMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 工具调用审计统计服务（spec §6 M10）。
 *
 * 维度：
 *   - byStatus : PENDING / EXECUTED / REJECTED / FAILED 计数
 *   - byTool   : 各 toolName 计数
 *   - byDay    : 按天计数（范围 [today-days+1, today]）
 *
 * tenant 隔离：mapper 由 TenantMybatisInterceptor 自动加 tenant_id 条件。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ToolAuditStatsService {

    private final ToolCallAuditMapper auditMapper;

    public AuditStatsVO stats(int days) {
        TenantContext.requireCurrent(); // 触发 tenant 上下文检查
        List<Map<String, Object>> statusRows = auditMapper.countByStatus();
        List<Map<String, Object>> toolRows = auditMapper.countByTool();
        LocalDate since = LocalDate.now().minusDays(Math.max(days, 1) - 1L);
        List<Map<String, Object>> dayRows = auditMapper.countByDay(since);

        Map<String, Long> byStatus = toCountMap(statusRows, "status");
        Map<String, Long> byTool = toCountMap(toolRows, "tool_name");
        Map<String, Long> byDay = toCountMap(dayRows, "day");

        long total = byStatus.values().stream().mapToLong(Long::longValue).sum();

        return AuditStatsVO.builder()
                .days(days)
                .total(total)
                .byStatus(byStatus)
                .byTool(byTool)
                .byDay(byDay)
                .build();
    }

    /**
     * 把 [{key=val, cnt=n}, ...] 转成 {key=n, ...}。
     * 数字统一转 Long；key 走 toString()。
     */
    private static Map<String, Long> toCountMap(List<Map<String, Object>> rows, String keyField) {
        Map<String, Long> out = new LinkedHashMap<>();
        if (rows == null) return out;
        for (Map<String, Object> row : rows) {
            Object k = row.get(keyField);
            Object n = row.get("cnt");
            if (k == null) continue;
            long count = n == null ? 0L : ((Number) n).longValue();
            out.put(k.toString(), count);
        }
        return out;
    }
}
