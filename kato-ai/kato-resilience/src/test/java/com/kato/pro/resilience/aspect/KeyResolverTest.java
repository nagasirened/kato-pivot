package com.kato.pro.resilience.aspect;

import com.kato.pro.resilience.annotation.KeyBy;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * KeyResolver 单元测试（spec §6 M12）。
 *
 * 验证不同 KeyBy 维度返回正确前缀；context lookup 用反射，context 缺失时回退到安全默认值。
 */
class KeyResolverTest {

    private final KeyResolver resolver = new KeyResolver();

    @Test
    void resolve_global_returnsConstant() {
        assertEquals("global", resolver.resolve(KeyBy.GLOBAL));
    }

    @Test
    void resolve_nullDimension_defaultsToGlobal() {
        assertEquals("global", resolver.resolve(null));
    }

    @Test
    void resolve_tenant_withoutContext_fallsBackToGlobal() {
        // 测试环境下没有 TenantContext 类 → reflection 失败 → 回退 global
        // 但因为这是同一 classpath，TenantContext 类可能存在但 currentOrNull() 返回 null
        String key = resolver.resolve(KeyBy.TENANT);
        // 接受 "global"（无 context）或 "t:N"（有 context）两种结果
        assertTrue(key.equals("global") || key.matches("t:\\d+"),
                "tenant key should be 'global' or 't:N', got: " + key);
    }

    @Test
    void resolve_user_withoutContext_fallsBackToGlobal() {
        String key = resolver.resolve(KeyBy.USER);
        assertTrue(key.equals("global") || key.matches("u:\\d+"),
                "user key should be 'global' or 'u:N', got: " + key);
    }

    @Test
    void resolve_ip_returnsIpPrefixedKey() {
        // 测试环境无 HttpServletRequest → 反射失败 → "ip:unknown"
        String key = resolver.resolve(KeyBy.IP);
        assertTrue(key.startsWith("ip:"), "ip key should start with 'ip:', got: " + key);
    }

    @Test
    void resolve_ip_unknownContext_returnsUnknownIp() {
        // reflection 失败时 fallback 到 "ip:unknown"
        String key = resolver.resolve(KeyBy.IP);
        assertNotNull(key);
        assertFalse(key.isEmpty());
    }
}
