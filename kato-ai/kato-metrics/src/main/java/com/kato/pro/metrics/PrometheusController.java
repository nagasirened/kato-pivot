package com.kato.pro.metrics;

import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.exporter.common.TextFormat;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.io.StringWriter;

/**
 * Prometheus 标准 endpoint（spec §6 M12）。
 *
 *   GET /actuator/prometheus → text/plain; version=0.0.4
 *
 * 路径可通过 kato.metrics.path 自定义。
 * 输出符合 Prometheus exposition format，可被 Prometheus server 直接 scrape。
 */
@RestController
@RequiredArgsConstructor
public class PrometheusController {

    private final CollectorRegistry collectorRegistry;
    private final MetricsProperties properties;

    @GetMapping(value = "${kato.metrics.path:/actuator/prometheus}",
                produces = "text/plain; version=0.0.4; charset=utf-8")
    public String prometheus() throws IOException {
        StringWriter sw = new StringWriter();
        TextFormat.write004(sw, collectorRegistry.metricFamilySamples());
        return sw.toString();
    }
}
