package com.kato.pro.langchain.infrastructure.external.minimax;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * MiniMax API 客户端配置。base-url/apiKey 留空由用户后续配置；M2 提供默认值便于开发。
 */
@Data
@ConfigurationProperties(prefix = "minimax")
public class MiniMaxProperties {

    /** API key. v1 留空；运行时注入（M2 不强制要求非空 — 单元测试用 stub）。 */
    private String apiKey = "";

    /** API base URL. MiniMax 兼容 OpenAI 协议。 */
    private String baseUrl = "https://api.minimax.chat/v1";

    private int connectTimeoutMs = 5_000;
    private int readTimeoutMs = 60_000;
}
