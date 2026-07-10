package com.kato.pro.langchain.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI 入口（spec §6 M10）。
 *
 * 启动后访问：
 *   - http://localhost:8080/v3/api-docs       OpenAPI JSON
 *   - http://localhost:8080/swagger-ui.html   Swagger UI
 *   - http://localhost:8080/doc.html          Knife4j UI（推荐，UI 更友好）
 *
 * application.yml 中 knife4j.enable=true 启用。
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI katoLangChainOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Kato LangChain API")
                        .description("AI 客服系统 API 文档（kato-ai/kato-langchain）")
                        .version("v1.0.0")
                        .contact(new Contact()
                                .name("Kato Team")
                                .email("dev@kato.local"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0")));
    }
}
