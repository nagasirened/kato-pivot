package com.kato.pro.metrics;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 简易健康检查（spec §6 M12）。
 *
 *   GET /actuator/health → { "status": "UP" }
 *
 * 替代 spring-boot-starter-actuator 的 health endpoint（避免 m2 缺 3.x 完整包）。
 */
@RestController
public class HealthController {

    @GetMapping("/actuator/health")
    public Map<String, Object> health() {
        return Map.of(
                "status", "UP",
                "module", "kato-metrics",
                "timestamp", System.currentTimeMillis()
        );
    }
}
