package com.kato.pro.risk.client.feign;

import com.kato.pro.risk.client.dto.GroovyTestRequest;
import com.kato.pro.risk.client.dto.GroovyTestResult;
import com.kato.pro.risk.client.dto.RiskRequest;
import com.kato.pro.risk.client.dto.RiskResponse;
import org.springframework.stereotype.Component;

/**
 * RiskFeignClient 的 Fallback 实现。
 * Resilience4j 熔断触发或网络异常时，fail-open 返回 PASS。
 *
 * Phase 1 骨架。
 */
@Component
public class FallbackRiskFeignClient implements RiskFeignClient {

    @Override
    public RiskResponse check(RiskRequest request) {
        return RiskResponse.failOpen(
                request == null || request.getRequestId() == null ? "unknown" : request.getRequestId(),
                "RISK_ENGINE_UNAVAILABLE"
        );
    }

    @Override
    public GroovyTestResult testGroovyScript(GroovyTestRequest request) {
        GroovyTestResult r = new GroovyTestResult();
        r.setTestCaseName("fallback");
        r.setPassed(false);
        r.setErrorMessage("RISK_ENGINE_UNAVAILABLE");
        return r;
    }
}