package com.kato.pro.metrics;

import io.prometheus.client.CollectorRegistry;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * kato-metrics 自动装配入口（spec §6 M12）。
 *
 * 引入本 module 即获得：
 *   - GET /actuator/prometheus 标准 endpoint
 *   - GET /actuator/health 简化版
 *   - MetricsRegistry bean（Prometheus 实现）
 */
@Configuration
@EnableConfigurationProperties(MetricsProperties.class)
public class KatoMetricsAutoConfiguration {

    @Bean
    public CollectorRegistry katoCollectorRegistry() {
        return CollectorRegistry.defaultRegistry;
    }

    @Bean
    public MetricsRegistry katoMetricsRegistry(CollectorRegistry registry, MetricsProperties properties) {
        return new PrometheusMetricsRegistry(registry, properties.getNamespace());
    }
}
