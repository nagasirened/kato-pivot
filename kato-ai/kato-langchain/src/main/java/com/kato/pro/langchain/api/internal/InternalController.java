package com.kato.pro.langchain.api.internal;

import com.kato.pro.langchain.common.metrics.MetricRegistry;
import com.kato.pro.langchain.common.result.Result;
import com.kato.pro.langchain.common.security.RequireRole;
import com.kato.pro.langchain.common.security.Role;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 内部监控 API（spec §6 M11）。
 *
 *   GET /api/v1/internal/metrics     全部指标快照（ADMIN/OPERATOR）
 *   GET /api/v1/internal/health      健康检查（v1 简化：返回 ok）
 *
 * v1：JSON 格式输出；v2 切 Prometheus text format（/actuator/prometheus）。
 * 鉴权：要求 ADMIN 或 OPERATOR；生产应额外加 IP 白名单。
 */
@Slf4j
@Tag(name = "Internal", description = "内部探活 / 指标端点（运维用）")
@RestController
@RequestMapping("/api/v1/internal")
public class InternalController {

    @Operation(operationId = "Health", summary = "健康探活",
            description = "进程存活探针（无鉴权）")
    @GetMapping("/health")
    public Result<Map<String, Object>> health() {
        return Result.ok(Map.of(
                "status", "UP",
                "timestamp", System.currentTimeMillis()
        ));
    }

    @Operation(operationId = "Metrics", summary = "内部指标摘要",
            description = "基础运行指标")
    @GetMapping("/metrics")
    @RequireRole({Role.ADMIN, Role.OPERATOR})
    public Result<List<MetricRegistry.MetricSnapshot>> metrics() {
        return Result.ok(MetricRegistry.snapshot());
    }
}
