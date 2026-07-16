package com.kato.pro.langchain.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI 入口（spec §6 M13）。
 *
 * 启动后访问：
 *   - http://localhost:8080/v3/api-docs       OpenAPI JSON
 *   - http://localhost:8080/swagger-ui.html   Swagger UI
 *   - http://localhost:8080/doc.html          Knife4j UI（推荐，UI 更友好）
 *
 * 文档元数据：
 *   - 全局 BearerAuth security scheme（JWT，由 JwtTokenFilter 注入）
 *   - dev / prod 两套 server URL
 *   - externalDocs 链接到设计 spec
 */
@Configuration
public class OpenApiConfig {

    /** JWT Bearer 鉴权 scheme 名（M11 JwtTokenFilter 颁发，HttpSecurity 校验） */
    public static final String SECURITY_SCHEME_NAME = "BearerAuth";

    @Bean
    public OpenAPI katoLangChainOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Kato LangChain API")
                        .description("AI 客服系统 API 文档（kato-ai/kato-langchain）。\n\n"
                                + "鉴权：除 /api/v1/auth/login 外所有端点需在 Header 带 `Authorization: Bearer <jwt>`。\n"
                                + "限流：chat.* 默认 20 QPS/租户，admin 接口受 RBAC 保护。")
                        .version("v1.0.0")
                        .contact(new Contact()
                                .name("Kato Team")
                                .email("dev@kato.local"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0")))
                .servers(List.of(
                        new Server().url("http://localhost:8080").description("本地开发"),
                        new Server().url("https://api.kato.local").description("生产环境")))
                .externalDocs(new ExternalDocumentation()
                        .description("Kato LangChain 设计 spec")
                        .url("https://kato.local/docs/spec/2026-07-07-ai-customer-service-design.html"))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME, new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("JWT 颁发见 /api/v1/auth/login；token 默认 24h 有效")))
                // 全局默认要求 BearerAuth；个别端点可用 @SecurityRequirements 覆盖
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME));
    }
}
