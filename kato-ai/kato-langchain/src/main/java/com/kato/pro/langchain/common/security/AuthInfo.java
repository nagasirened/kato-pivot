package com.kato.pro.langchain.common.security;

/**
 * 当前请求的鉴权快照（不可变）。由 JwtAuthFilter 构造后放入 AuthContext。
 *
 * 与 TenantContext 解耦：auth 解析失败 → AuthContext 为空；
 * 但 TenantContext 仍可被 TenantContextFilter 填充（如果业务需要匿名租户）。
 */
public record AuthInfo(Long userId, String username, Long tenantId, Role role) {

    public AuthInfo {
        if (userId == null) throw new IllegalArgumentException("userId must not be null");
        if (username == null) throw new IllegalArgumentException("username must not be null");
        if (tenantId == null) throw new IllegalArgumentException("tenantId must not be null");
        if (role == null) role = Role.USER;
    }

    public static AuthInfo of(Long userId, String username, Long tenantId, Role role) {
        return new AuthInfo(userId, username, tenantId, role);
    }
}
