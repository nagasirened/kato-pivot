package com.kato.pro.metrics;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * kato-metrics 配置（kato.metrics.*）。
 *
 *   - enabled    : 是否启用 Prometheus 端点（默认 true）
 *   - path       : endpoint 路径（默认 /actuator/prometheus）
 *   - namespace  : 所有指标前缀（默认 "kato"；最终指标名 = "kato_<metricName>"）
 */
@Data
@ConfigurationProperties(prefix = "kato.metrics")
public class MetricsProperties {
    private boolean enabled = true;
    private String path = "/actuator/prometheus";
    private String namespace = "kato";
}
