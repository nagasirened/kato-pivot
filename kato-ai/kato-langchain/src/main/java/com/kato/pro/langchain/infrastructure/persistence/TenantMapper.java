package com.kato.pro.langchain.infrastructure.persistence;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kato.pro.langchain.common.entity.Tenant;
import org.apache.ibatis.annotations.Mapper;

/**
 * Tenant 表的 Mapper。**注意**：Tenant 是租户维度表，不参与多租户过滤，
 * 因此使用普通 BaseMapper，不继承 TenantAwareBaseMapper。
 * 它对应的 SQL 在 TenantMybatisInterceptor 中通过表名豁免放行。
 */
@Mapper
public interface TenantMapper extends BaseMapper<Tenant> {
}
