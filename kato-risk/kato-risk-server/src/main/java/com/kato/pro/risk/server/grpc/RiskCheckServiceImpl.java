package com.kato.pro.risk.server.grpc;

import com.kato.pro.risk.client.dto.RiskRequest;
import com.kato.pro.risk.client.dto.RiskResponse;
import com.kato.pro.risk.grpc.GrpcGroovyTestRequest;
import com.kato.pro.risk.grpc.GrpcGroovyTestResult;
import com.kato.pro.risk.grpc.GrpcRiskRequest;
import com.kato.pro.risk.grpc.GrpcRiskResponse;
import com.kato.pro.risk.grpc.RiskCheckServiceGrpc;
import com.kato.pro.risk.server.service.RuleEngineService;
import com.kato.pro.risk.server.algorithm.GroovyScriptExecutor;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * gRPC 风控服务实现。
 * 将 gRPC 请求转换为 RuleEngineService.check() 调用，结果映射回 GrpcRiskResponse。
 */
@Slf4j
@Component
public class RiskCheckServiceImpl extends RiskCheckServiceGrpc.RiskCheckServiceImplBase {

    @javax.inject.Inject
    private RuleEngineService ruleEngineService;
    @javax.inject.Inject
    private GroovyScriptExecutor groovyScriptExecutor;

    @Override
    public void check(GrpcRiskRequest request, StreamObserver<GrpcRiskResponse> responseObserver) {
        String traceId = MDC.get("traceId");
        log.info("[gRPC check] traceId={}, requestId={}, scene={}, userId={}",
                traceId, request.getRequestId(), request.getScene(), request.getUserId());

        try {
            RiskRequest riskRequest = convert(request);
            RiskResponse riskResponse = ruleEngineService.check(riskRequest);
            GrpcRiskResponse.Builder builder = GrpcRiskResponse.newBuilder()
                    .setRequestId(riskResponse.getRequestId() == null ? "" : riskResponse.getRequestId())
                    .setAction(riskResponse.getAction() == null ? "PASS" : riskResponse.getAction())
                    .setMessage(riskResponse.getMessage() == null ? "" : riskResponse.getMessage());
            if (riskResponse.getScore() != null) {
                builder.setScore(riskResponse.getScore());
            }
            if (riskResponse.getReasonCodes() != null) {
                builder.addAllReasonCodes(riskResponse.getReasonCodes());
            }
            if (riskResponse.getExtData() != null) {
                builder.putAllExtData(riskResponse.getExtData());
            }
            responseObserver.onNext(builder.build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("[gRPC check] requestId={} failed", request.getRequestId(), e);
            GrpcRiskResponse errorResponse = GrpcRiskResponse.newBuilder()
                    .setRequestId(request.getRequestId())
                    .setAction("PASS")
                    .setScore(0)
                    .setMessage("internal error: " + e.getMessage())
                    .build();
            responseObserver.onNext(errorResponse);
            responseObserver.onCompleted();
        }
    }

    @Override
    public void testGroovyScript(GrpcGroovyTestRequest request,
                                  StreamObserver<GrpcGroovyTestResult> responseObserver) {
        String traceId = MDC.get("traceId");
        log.info("[gRPC testGroovyScript] traceId={}, testCase={}",
                traceId, request.getTestCaseName());

        long startMs = System.currentTimeMillis();
        try {
            // 构建 RiskRequest（仅用于 ctx binding）
            RiskRequest riskRequest = buildTestRequest(request.getInputContextMap());

            // 执行 Groovy 脚本（ruleId=-1 表示测试脚本，不走缓存）
            Map<String, Object> groovyResult = groovyScriptExecutor.execute(
                    -1L, request.getScriptContent(), riskRequest);

            String action = String.valueOf(groovyResult.getOrDefault("action", "PASS"));
            boolean passed = !"PASS".equals(action) ? false : true;
            long execMs = System.currentTimeMillis() - startMs;

            GrpcGroovyTestResult result = GrpcGroovyTestResult.newBuilder()
                    .setTestCaseName(request.getTestCaseName())
                    .setPassed(passed)
                    .setOutput(action + " | score=" + groovyResult.getOrDefault("score", 0.0))
                    .setExecutionTimeMs(execMs)
                    .build();
            responseObserver.onNext(result);
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("[gRPC testGroovyScript] testCase={} failed", request.getTestCaseName(), e);
            long execMs = System.currentTimeMillis() - startMs;
            GrpcGroovyTestResult result = GrpcGroovyTestResult.newBuilder()
                    .setTestCaseName(request.getTestCaseName())
                    .setPassed(false)
                    .setErrorMessage(e.getMessage())
                    .setExecutionTimeMs(execMs)
                    .build();
            responseObserver.onNext(result);
            responseObserver.onCompleted();
        }
    }

    private RiskRequest buildTestRequest(Map<String, String> inputContext) {
        RiskRequest req = new RiskRequest();
        if (inputContext != null) {
            req.setUserId(inputContext.get("userId"));
            req.setDeviceId(inputContext.get("deviceId"));
            req.setIp(inputContext.get("ip"));
            req.setScene(inputContext.get("scene"));
            req.setRequestId(inputContext.get("requestId"));
            if (inputContext.containsKey("orderAmount")) {
                try {
                    req.setOrderAmount(new java.math.BigDecimal(inputContext.get("orderAmount")));
                } catch (NumberFormatException ignored) {}
            }
            req.setShippingAddress(inputContext.get("shippingAddress"));
        }
        return req;
    }

    private RiskRequest convert(GrpcRiskRequest grpc) {
        RiskRequest req = new RiskRequest();
        req.setRequestId(grpc.getRequestId());
        req.setScene(grpc.getScene());
        req.setUserId(grpc.getUserId());
        req.setDeviceId(grpc.getDeviceId());
        req.setIp(grpc.getIp());
        req.setExtData(new HashMap<>(grpc.getExtDataMap()));
        return req;
    }
}