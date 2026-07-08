package com.kato.pro.langchain.infrastructure.persistence;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import java.io.Serializable;
import java.util.Collection;

/**
 * 多租户安全 Mapper 基类（标记接口）。
 *
 * 重要：实际租户 SQL 拦截由 TenantMybatisInterceptor 强制执行；
 * 本接口作为标记，便于将来扫描哪些 Mapper 需要租户增强。
 *
 * 用法：
 *   - 默认 CRUD：extends TenantAwareBaseMapper&lt;BaseEntity 子类&gt;，无需额外处理
 *   - 复杂 SQL（如多表 JOIN、原生 XML）：必须在 SQL 中显式带 tenant_id 过滤条件
 *     （拦截器会在运行时校验 XML SQL 是否带 tenant_id，否则抛 SystemException）。
 */
public interface TenantAwareBaseMapper<T> extends BaseMapper<T> {

    /** 显式安全 API：按 id 批量查询（仅返回当前租户的记录）。 */
    default java.util.List<T> selectBatchIdsSafe(Collection<? extends Serializable> idList) {
        return selectBatchIds((Collection<? extends Long>) idList);
    }
}
