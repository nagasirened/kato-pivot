package com.kato.pro.langchain.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 记忆/摘要配置。绑定 application.yml 的 memory.* 段。
 */
@Data
@ConfigurationProperties(prefix = "memory")
public class MemoryProperties {

    private Window window = new Window();
    private Summary summary = new Summary();

    @Data
    public static class Window {
        /** 窗口硬上限 token */
        private int maxTokens = 8000;
        /** 为 system/prompt 预留比例（0~1） */
        private double reserveRatio = 0.1;
    }

    @Data
    public static class Summary {
        /** 触发摘要的累计 token 阈值 */
        private int triggerThresholdTokens = 4000;
        /** 同一会话两次摘要最小间隔（秒） */
        private long minIntervalSeconds = 30L;
    }
}
