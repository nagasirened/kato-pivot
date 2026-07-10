package com.kato.pro.langchain.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@Data
@ConfigurationProperties(prefix = "safety")
public class SafetyProperties {

    private boolean enabled = true;
    private LocalKeywords localKeywords = new LocalKeywords();
    private SensitiveClient sensitiveClient = new SensitiveClient();

    @Data
    public static class LocalKeywords {
        private boolean enabled = true;
        /** 默认兜底关键词（可在 yml 覆盖） */
        private List<String> defaultWords = new ArrayList<>(List.of("色情", "赌博", "毒品"));
    }

    @Data
    public static class SensitiveClient {
        /** HTTP 客户端实现是否启用 */
        private boolean enabled = false;
        /** kato-sensitive-client 地址 */
        private String baseUrl = "http://localhost:8081";
        private int timeoutMs = 1000;
    }
}
