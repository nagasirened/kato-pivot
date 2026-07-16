package com.kato.pro.resilience.aspect;

import com.kato.pro.resilience.annotation.KeyBy;
// 反射获取 HttpServletRequest，避免对 javax/jakarta servlet 的硬依赖
// （kato-pivot 根 pom 还在用 spring-boot 2.5.5 的 javax.servlet）

/**
 * Key 解析器（spec §6 M12）。
 *
 * 默认实现：从 TenantContext / AuthContext / HttpServletRequest 提取限流 key。
 * 业务方可在自己的 starter / config 中覆盖。
 */
public class KeyResolver {

    /**
     * 解析限流 key。
     *
     * @return key 字符串（不同 key 维度返回不同前缀避免冲突）
     */
    public String resolve(KeyBy by) {
        if (by == null) by = KeyBy.GLOBAL;
        switch (by) {
            case TENANT: {
                Long t = currentTenantId();
                return t == null ? "global" : "t:" + t;
            }
            case USER: {
                Long u = currentUserId();
                return u == null ? "global" : "u:" + u;
            }
            case IP: {
                return "ip:" + currentIp();
            }
            case GLOBAL:
            default:
                return "global";
        }
    }

    // ---- context lookup helpers (reflection to avoid hard dep on langchain) ----

    private static Long currentTenantId() {
        try {
            Class<?> tc = Class.forName("com.kato.pro.langchain.common.tenant.TenantContext");
            Object info = tc.getMethod("currentOrNull").invoke(null);
            if (info == null) return null;
            return (Long) info.getClass().getMethod("tenantId").invoke(info);
        } catch (Exception e) {
            return null;
        }
    }

    private static Long currentUserId() {
        try {
            Class<?> ac = Class.forName("com.kato.pro.langchain.common.security.AuthContext");
            Object info = ac.getMethod("currentOrNull").invoke(null);
            if (info == null) return null;
            return (Long) info.getClass().getMethod("userId").invoke(info);
        } catch (Exception e) {
            return null;
        }
    }

    private static String currentIp() {
        try {
            Object attrs = Class.forName("org.springframework.web.context.request.RequestContextHolder")
                    .getMethod("getRequestAttributes").invoke(null);
            if (attrs == null) return "unknown";
            Object req = attrs.getClass().getMethod("getRequest").invoke(attrs);
            if (req == null) return "unknown";
            // X-Forwarded-For 优先
            Object xff = req.getClass().getMethod("getHeader", String.class).invoke(req, "X-Forwarded-For");
            if (xff instanceof String s && !s.isBlank()) {
                int comma = s.indexOf(',');
                return (comma > 0 ? s.substring(0, comma) : s).trim();
            }
            Object ip = req.getClass().getMethod("getRemoteAddr").invoke(req);
            return ip == null ? "unknown" : ip.toString();
        } catch (Exception e) {
            return "unknown";
        }
    }
}
