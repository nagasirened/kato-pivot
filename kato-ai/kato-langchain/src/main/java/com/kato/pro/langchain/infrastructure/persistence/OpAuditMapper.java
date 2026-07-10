package com.kato.pro.langchain.infrastructure.persistence;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.kato.pro.langchain.domain.audit.OpAudit;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface OpAuditMapper extends TenantAwareBaseMapper<OpAudit> {

    default IPage<OpAudit> pageByUser(Long tenantId, Long userId, String action, String resource,
                                       int page, int size) {
        QueryWrapper<OpAudit> q = new QueryWrapper<>();
        if (userId != null) q.eq("user_id", userId);
        if (action != null && !action.isBlank()) q.eq("action", action);
        if (resource != null && !resource.isBlank()) q.eq("resource", resource);
        q.orderByDesc("create_time");
        return selectPage(new Page<>(page, size), q);
    }

    default List<OpAudit> recentByResource(Long tenantId, String resource, int limit) {
        QueryWrapper<OpAudit> q = new QueryWrapper<OpAudit>()
                .eq("resource", resource)
                .orderByDesc("create_time")
                .last("LIMIT " + Math.min(Math.max(limit, 1), 200));
        return selectList(q);
    }
}
