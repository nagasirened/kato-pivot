package com.kato.pro.langchain.domain.tool;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Tool 框架配置（tool.*）。
 *
 *   - enabled              : 全局开关；false 时 dispatcher 直接拒绝
 *   - default-timeout-ms   : ToolInvoker 超时（v1 用 CompletableFuture）
 *   - cb-failure-threshold : 熔断触发失败次数（默认 5）
 *   - cb-reset-ms          : 熔断半开等待时间（默认 30s）
 *   - audit-auto-approve   : WRITE 工具自动通过（v1 默认 false；M10 接审核工作台再开）
 */
@Data
@ConfigurationProperties(prefix = "tool")
public class ToolProperties {
    private boolean enabled = true;
    private long defaultTimeoutMs = 1000;
    private int cbFailureThreshold = 5;
    private long cbResetMs = 30_000;
    private boolean auditAutoApprove = false;
}
