package com.kato.pro.langchain.common.tenant;

/**
 * 当前请求的租户上下文快照（不可变）。由 TenantContextFilter 构造后放入 TenantContext。
 */
public record TenantInfo(Long tenantId, Long userId, String channel) {

    public TenantInfo {
        if (tenantId == null) {
            throw new IllegalArgumentException("tenantId must not be null");
        }
        if (userId == null) {
            throw new IllegalArgumentException("userId must not be null");
        }
    }

    public static TenantInfo of(Long tenantId, Long userId) {
        return new TenantInfo(tenantId, userId, "DEFAULT");
    }
}
