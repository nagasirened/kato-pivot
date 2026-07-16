package com.kato.pro.resilience.annotation;

/**
 * 限流 / 熔断 key 维度（spec §6 M12）。
 *
 *   - TENANT : 从 TenantContext 取（多租户隔离 QPS）
 *   - USER   : 从 AuthContext 取（单用户限速）
 *   - IP     : 从 HttpServletRequest.getRemoteAddr() 取
 *   - GLOBAL : 全局共享一份
 */
public enum KeyBy {
    TENANT,
    USER,
    IP,
    GLOBAL
}
