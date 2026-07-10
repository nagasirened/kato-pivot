package com.kato.pro.langchain.infrastructure.persistence;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.kato.pro.langchain.domain.tool.TenantToolConfig;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Optional;

@Mapper
public interface TenantToolConfigMapper extends TenantAwareBaseMapper<TenantToolConfig> {

    default Optional<TenantToolConfig> findByToolName(Long tenantId, String toolName) {
        TenantToolConfig r = selectOne(new QueryWrapper<TenantToolConfig>()
                .eq("tool_name", toolName));
        return Optional.ofNullable(r);
    }

    default List<TenantToolConfig> listByTenant(Long tenantId) {
        return selectList(new QueryWrapper<TenantToolConfig>()
                .orderByAsc("tool_name"));
    }
}
