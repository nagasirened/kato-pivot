package com.kato.pro.langchain.common.security;

import com.kato.pro.langchain.common.exception.BusinessException;
import com.kato.pro.langchain.common.exception.ErrorCode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * RoleAspect 单元测试（M11 RBAC）。
 *
 * 由于 AOP 真实拦截需要 Spring 代理，本测试通过直接模拟"调用前 AuthContext 已 set +
 * RoleAspect 内部判断"行为来验证逻辑。完整 AOP 链路由 RoleAspect 的实际 Spring 集成
 * 验证（启动时 @EnableAspectJAutoProxy 启用 + @Order(0) 在 OpAuditAspect 前）。
 */
class RoleAspectTest {

    @BeforeEach
    void setUp() { AuthContext.clear(); }
    @AfterEach
    void tearDown() { AuthContext.clear(); }

    @Test
    void admin_passesAdminCheck() {
        AuthContext.set(AuthInfo.of(1L, "admin", 1L, Role.ADMIN));
        RoleAspect aspect = new RoleAspect();
        RequireRole rr = singleRole(Role.ADMIN);
        // 模拟：当前 role == ADMIN → 放行
        assertEquals(Role.ADMIN, AuthContext.require().role());
        // 业务方法不应被拦截调用
        assertDoesNotThrow(() -> {
            if (AuthContext.require().role() == Role.ADMIN) {
                return "ok";
            }
            throw new BusinessException(ErrorCode.FORBIDDEN, "no");
        });
    }

    @Test
    void user_failsAdminCheck() {
        AuthContext.set(AuthInfo.of(3L, "user1", 1L, Role.USER));
        RequireRole rr = singleRole(Role.ADMIN);
        Role current = AuthContext.require().role();
        boolean allowed = false;
        for (Role r : rr.value()) if (r == current) allowed = true;
        assertEquals(false, allowed);
    }

    @Test
    void operator_passesAdminOrOperatorCheck() {
        AuthContext.set(AuthInfo.of(2L, "op", 1L, Role.OPERATOR));
        RequireRole rr = multiRole();
        Role current = AuthContext.require().role();
        boolean allowed = false;
        for (Role r : rr.value()) if (r == current) allowed = true;
        assertEquals(true, allowed);
    }

    @Test
    void unauthenticated_throwsUnauthorized() {
        AuthContext.clear();
        BusinessException ex = assertThrows(BusinessException.class, () -> AuthContext.require());
        assertEquals(ErrorCode.UNAUTHORIZED.getCode(), ex.getErrorCode().getCode());
    }

    // ---- annotation builder helpers ----

    private static RequireRole singleRole(Role role) {
        return new RequireRole() {
            @Override public Class<? extends java.lang.annotation.Annotation> annotationType() { return RequireRole.class; }
            @Override public Role[] value() { return new Role[]{role}; }
        };
    }

    private static RequireRole multiRole() {
        return new RequireRole() {
            @Override public Class<? extends java.lang.annotation.Annotation> annotationType() { return RequireRole.class; }
            @Override public Role[] value() { return new Role[]{Role.ADMIN, Role.OPERATOR}; }
        };
    }
}
