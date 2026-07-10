package com.kato.pro.langchain.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

/**
 * 安全 / AOP 配置（M11）。
 *
 * v1 故意不引入 spring-security（沙箱无 3.x 依赖），仅启用 AOP。
 * JwtAuthFilter（WebConfig 注册）+ @RequireRole + RoleAspect 已构成最小 RBAC。
 */
@Configuration
@EnableAspectJAutoProxy
public class SecurityConfig {
}
