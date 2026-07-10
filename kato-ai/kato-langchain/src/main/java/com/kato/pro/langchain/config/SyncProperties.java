package com.kato.pro.langchain.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Sync 配置（sync.*）。
 *
 *   - enabled         : 全局开关（false 时禁用所有定时同步；手动触发仍可用）
 *   - cron            : Spring cron 表达式（默认每 2 小时）
 *   - default-tenant  : 默认租户 ID（v1 单租户简化）
 *   - retry-times     : 单条 record 失败重试次数（默认 3；v1 暂未实装重试）
 *   - keep-runs       : 每个 adapter 保留最近 run 记录数（默认 20）
 */
@Data
@ConfigurationProperties(prefix = "sync")
public class SyncProperties {
    private boolean enabled = true;
    private String cron = "0 0 */2 * * *";
    private Long defaultTenant = 1L;
    private int retryTimes = 3;
    private int keepRuns = 20;
}
