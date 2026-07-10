package com.kato.pro.langchain.infrastructure.persistence;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.kato.pro.langchain.domain.tool.ToolCallAudit;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

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
}
