package com.kato.pro.langchain.infrastructure.persistence;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.kato.pro.langchain.domain.tool.ToolCallAudit;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Mapper
public interface ToolCallAuditMapper extends TenantAwareBaseMapper<ToolCallAudit> {

    default IPage<ToolCallAudit> pageByStatus(Long tenantId, String status,
                                              String toolName, int page, int size) {
        QueryWrapper<ToolCallAudit> q = new QueryWrapper<>();
        if (status != null && !status.isBlank()) q.eq("status", status);
        if (toolName != null && !toolName.isBlank()) q.eq("tool_name", toolName);
        q.orderByDesc("create_time");
        return selectPage(new Page<>(page, size), q);
    }

    default List<ToolCallAudit> listPending(Long tenantId, int limit) {
        QueryWrapper<ToolCallAudit> q = new QueryWrapper<ToolCallAudit>()
                .eq("status", "PENDING")
                .orderByAsc("create_time")
                .last("LIMIT " + Math.min(Math.max(limit, 1), 200));
        return selectList(q);
    }

    /**
     * 按 status 聚合计数（tenant 隔离由 TenantMybatisInterceptor 保证）。
     * 返回 [{status=EXECUTED, cnt=120}, ...]
     */
    default List<Map<String, Object>> countByStatus() {
        QueryWrapper<ToolCallAudit> q = new QueryWrapper<ToolCallAudit>()
                .select("status", "count(*) AS cnt")
                .groupBy("status");
        return selectMaps(q);
    }

    /** 按 tool_name 聚合计数 */
    default List<Map<String, Object>> countByTool() {
        QueryWrapper<ToolCallAudit> q = new QueryWrapper<ToolCallAudit>()
                .select("tool_name", "count(*) AS cnt")
                .groupBy("tool_name");
        return selectMaps(q);
    }

    /**
     * 按天聚合计数（仅返回 ≥ sinceDay）。
     * 返回 [{day=2026-07-04, cnt=10}, ...]（key 已被 MySQL 别名为小写 day）
     */
    default List<Map<String, Object>> countByDay(LocalDate sinceDay) {
        QueryWrapper<ToolCallAudit> q = new QueryWrapper<ToolCallAudit>()
                .select("DATE(create_time) AS day", "count(*) AS cnt")
                .ge("create_time", sinceDay.atStartOfDay())
                .groupBy("DATE(create_time)")
                .orderByAsc("day");
        return selectMaps(q);
    }
}
