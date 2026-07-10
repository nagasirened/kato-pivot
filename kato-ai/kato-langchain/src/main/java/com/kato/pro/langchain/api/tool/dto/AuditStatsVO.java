package com.kato.pro.langchain.api.tool.dto;

import lombok.Builder;
import lombok.Value;

import java.util.Map;

/**
 * 工具调用审计统计（GET /api/v1/admin/tool/audit/stats）。
 *
 *   - total     : 总调用次数
 *   - byStatus  : {PENDING=3, EXECUTED=120, REJECTED=18, FAILED=1}
 *   - byTool    : {toolName=count}
 *   - byDay     : {yyyy-MM-dd=count}（含 days 天内）
 *   - days      : 查询范围
 */
@Value
@Builder
public class AuditStatsVO {
    int days;
    long total;
    Map<String, Long> byStatus;
    Map<String, Long> byTool;
    Map<String, Long> byDay;
}
