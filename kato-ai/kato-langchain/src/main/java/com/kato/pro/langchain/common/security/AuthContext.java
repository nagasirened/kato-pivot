package com.kato.pro.langchain.common.security;

import com.kato.pro.langchain.common.exception.BusinessException;
import com.kato.pro.langchain.common.exception.ErrorCode;

/**
 * 鉴权上下文 ThreadLocal 持有器（类比 TenantContext）。
 *
 * 关键规则：
 *   - 业务代码通过 require() / requireRole() 获取当前用户
 *   - JwtAuthFilter 在 finally 中调用 clear()，避免线程池复用泄漏
 *   - 未设置时直接抛 BusinessException(UNAUTHORIZED) — 不允许业务"匿名通过"
 */
public final class AuthContext {

    private static final ThreadLocal<AuthInfo> CONTEXT = new ThreadLocal<>();

    private AuthContext() {
    }

    public static void set(AuthInfo info) {
        CONTEXT.set(info);
    }

    /** 获取当前鉴权上下文；未设置时返回 null（仅用于 Filter 自身） */
    public static AuthInfo currentOrNull() {
        return CONTEXT.get();
    }

    /** 获取当前鉴权上下文；未设置时抛 UNAUTHORIZED */
    public static AuthInfo require() {
        AuthInfo info = CONTEXT.get();
        if (info == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录或登录已过期");
        }
        return info;
    }

    /** 获取当前角色；未设置时抛 UNAUTHORIZED */
    public static Role requireRole() {
        return require().role();
    }

    public static Long currentUserId() {
        return require().userId();
    }

    public static Long currentTenantId() {
        return require().tenantId();
    }

    public static void clear() {
        CONTEXT.remove();
    }
}
