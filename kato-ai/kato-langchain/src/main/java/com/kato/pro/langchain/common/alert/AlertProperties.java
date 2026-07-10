package com.kato.pro.langchain.common.alert;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 告警配置（alert.*）。
 *
 *   - enabled     : 总开关
 *   - interval-ms : 检查周期（默认 60s）
 *   - webhook-url : Webhook 目标 URL（空时走 LoggingAlertDispatcher）
 */
@Data
@ConfigurationProperties(prefix = "alert")
public class AlertProperties {
    private boolean enabled = true;
    private long intervalMs = 60_000;
    private String webhookUrl = "";
}
