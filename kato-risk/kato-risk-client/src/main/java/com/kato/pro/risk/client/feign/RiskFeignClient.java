package com.kato.pro.risk.client.feign;

import com.kato.pro.risk.client.dto.RiskRequest;
import com.kato.pro.risk.client.dto.RiskResponse;
import com.kato.pro.risk.client.dto.GroovyTestRequest;
import com.kato.pro.risk.client.dto.GroovyTestResult;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * 业务系统调用风控 Server 的 FeignClient 接口。
 *
 * Phase 1 骨架：
 * - check: 同步执行风控检查，返回 RiskResponse
 * - testGroovyScript: Groovy 脚本单元测试（给模拟输入，验证输出）
 *
 * fallback 由 FallbackRiskFeignClient 提供，Resilience4j 熔断触发时 fail-open（返回 PASS）。
 */
@FeignClient(
    name = "kato-risk-server",
    url = "${risk.server.url:http://localhost:8080}",
    fallback = FallbackRiskFeignClient.class
)
public interface RiskFeignClient {

    @PostMapping("/risk/check")
    RiskResponse check(@RequestBody RiskRequest request);

    @PostMapping("/risk/check/groovy")
    GroovyTestResult testGroovyScript(@RequestBody GroovyTestRequest request);
}
