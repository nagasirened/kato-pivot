package com.kato.pro.langchain.common.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 角色校验注解（M11 RBAC）。
 *
 * 标注在 controller 方法上：调用前由 RoleAspect 拦截，
 * 当前用户角色不匹配任一允许角色时抛 FORBIDDEN。
 *
 * 用例：
 *   @RequireRole(Role.ADMIN)                          仅管理员
 *   @RequireRole({Role.ADMIN, Role.OPERATOR})         管理员或运营
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireRole {
    Role[] value();
}
