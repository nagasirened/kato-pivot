package com.kato.pro.langchain.common.tenant;

import com.kato.pro.langchain.common.exception.BusinessException;
import com.kato.pro.langchain.common.exception.ErrorCode;

/**
 * 租户上下文 ThreadLocal 持有器。
 *
 * 当前实现：标准 ThreadLocal（沙箱环境下 transmittable-thread-local 不可用）。
 * 未来：升级为 TransmittableThreadLocal 以支持线程池任务传递（已在 plan 中标注）。
 *
 * 关键规则：
 *   - 任何 Service / Mapper 调用前必须能拿到 current()，否则视为严重 bug
 *   - Filter 必须在 finally 中调用 clear()，避免线程池复用泄漏
 */
public final class TenantContext {

    private static final ThreadLocal<TenantInfo> CONTEXT = new ThreadLocal<>();

    private TenantContext() {
    }

    public static void set(TenantInfo info) {
        CONTEXT.set(info);
    }

    /** 获取当前租户上下文。绝不允许返回 null——未设置时直接抛 BusinessException。 */
    public static TenantInfo requireCurrent() {
        TenantInfo info = CONTEXT.get();
        if (info == null) {
            throw new BusinessException(ErrorCode.TENANT_MISMATCH, "租户上下文未设置，请确认请求经过 TenantContextFilter");
        }
        return info;
    }

    /** 获取当前租户上下文；未设置时返回 null（仅用于 Filter 自身或启动期检查）。 */
    public static TenantInfo currentOrNull() {
        return CONTEXT.get();
    }

    public static Long currentTenantId() {
        return requireCurrent().tenantId();
    }

    public static Long currentUserId() {
        return requireCurrent().userId();
    }

    public static void clear() {
        CONTEXT.remove();
    }
}
