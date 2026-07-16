package com.kato.pro.langchain.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * OpenApiConfig 单元测试（spec §6 M13）。
 *
 * 验证全局 OpenAPI bean 元数据：
 *   - Info（title/description/version/license/contact）
 *   - Servers（dev + prod）
 *   - ExternalDocs 链接到设计 spec
 *   - SecurityScheme（BearerAuth/JWT）
 *   - 默认 security item（要求 BearerAuth）
 */
class OpenApiConfigTest {

    private final OpenApiConfig config = new OpenApiConfig();
    private final OpenAPI openApi = config.katoLangChainOpenAPI();

    @Test
    void info_containsTitleVersionAndLicense() {
        assertNotNull(openApi.getInfo());
        assertEquals("Kato LangChain API", openApi.getInfo().getTitle());
        assertEquals("v1.0.0", openApi.getInfo().getVersion());
        assertNotNull(openApi.getInfo().getLicense());
        assertEquals("Apache 2.0", openApi.getInfo().getLicense().getName());
        assertNotNull(openApi.getInfo().getContact());
        assertEquals("Kato Team", openApi.getInfo().getContact().getName());
    }

    @Test
    void servers_containsDevAndProd() {
        assertNotNull(openApi.getServers());
        assertEquals(2, openApi.getServers().size());
        var urls = openApi.getServers().stream().map(s -> s.getUrl()).toList();
        assertTrue(urls.contains("http://localhost:8080"), "should include local dev server");
        assertTrue(urls.contains("https://api.kato.local"), "should include prod server");
    }

    @Test
    void externalDocs_pointsToSpec() {
        assertNotNull(openApi.getExternalDocs());
        assertTrue(openApi.getExternalDocs().getUrl().contains("2026-07-07-ai-customer-service-design"),
                "externalDocs URL should reference the design spec, got: " + openApi.getExternalDocs().getUrl());
    }

    @Test
    void components_hasBearerAuthJwtScheme() {
        assertNotNull(openApi.getComponents());
        SecurityScheme scheme = openApi.getComponents()
                .getSecuritySchemes()
                .get(OpenApiConfig.SECURITY_SCHEME_NAME);
        assertNotNull(scheme, "BearerAuth scheme should be registered");
        assertEquals(SecurityScheme.Type.HTTP, scheme.getType());
        assertEquals("bearer", scheme.getScheme());
        assertEquals("JWT", scheme.getBearerFormat());
    }

    @Test
    void globalSecurityItem_requiresBearerAuthByDefault() {
        assertNotNull(openApi.getSecurity());
        assertFalse(openApi.getSecurity().isEmpty(),
                "default security should require BearerAuth");
        assertTrue(openApi.getSecurity().get(0).containsKey(OpenApiConfig.SECURITY_SCHEME_NAME),
                "default security item should reference BearerAuth");
    }

    @Test
    void securitySchemeName_isStableExportedConstant() {
        // 锁住 scheme name，避免后人在 OpenApiConfig 里改了但 SecurityItem 没改导致 NPE
        assertEquals("BearerAuth", OpenApiConfig.SECURITY_SCHEME_NAME);
    }
}
