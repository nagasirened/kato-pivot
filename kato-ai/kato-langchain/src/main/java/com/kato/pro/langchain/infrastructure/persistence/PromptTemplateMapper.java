package com.kato.pro.langchain.infrastructure.persistence;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.kato.pro.langchain.domain.prompt.PromptTemplate;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Optional;

@Mapper
public interface PromptTemplateMapper extends TenantAwareBaseMapper<PromptTemplate> {

    /** 取租户级覆盖；未找到返回 empty。注：tenant_id IS NULL 系统级不走这里。 */
    default Optional<PromptTemplate> findTenantOverride(Long tenantId, String key) {
        PromptTemplate r = selectOne(new QueryWrapper<PromptTemplate>()
                .eq("tenant_id", tenantId)
                .eq("template_key", key));
        return Optional.ofNullable(r);
    }

    /** 取所有非空模板（系统级 + 各租户），registry 启动加载用 */
    default List<PromptTemplate> loadAll() {
        return selectList(new QueryWrapper<PromptTemplate>()
                .orderByAsc("tenant_id", "template_key"));
    }
}
