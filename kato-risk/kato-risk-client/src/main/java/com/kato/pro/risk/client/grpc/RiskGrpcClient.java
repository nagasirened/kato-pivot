package com.kato.pro.risk.client.grpc;

import com.kato.pro.risk.client.dto.GroovyTestResult;
import com.kato.pro.risk.client.dto.RiskRequest;
import com.kato.pro.risk.client.dto.RiskResponse;
import com.kato.pro.risk.grpc.GrpcGroovyTestRequest;
import com.kato.pro.risk.grpc.GrpcGroovyTestResult;
import com.kato.pro.risk.grpc.GrpcRiskRequest;
import com.kato.pro.risk.grpc.GrpcRiskResponse;
import com.kato.pro.risk.grpc.RiskCheckServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.StatusRuntimeException;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * gRPC 客户端封装。
 * 内部自动获取 channel，支持 header 注入，与 FeignClient 并行运行（Phase 1 保留 Feign 作为 fallback）。
 */
@Slf4j
@Service
public class RiskGrpcClient {

    @Autowired
    private GrpcChannelManager channelManager;

    /**
     * 同步风控检查。
     */
    public RiskResponse check(RiskRequest request) {
        String traceId = MDC.get("traceId");
        String tenantId = MDC.get("tenantId");

        ManagedChannel channel = channelManager.getChannelWithHeaders(traceId, tenantId);
        try {
            RiskCheckServiceGrpc.RiskCheckServiceBlockingStub stub =
                    RiskCheckServiceGrpc.newBlockingStub(channel)
                            .withDeadlineAfter(3, TimeUnit.SECONDS);

            GrpcRiskRequest.Builder builder = GrpcRiskRequest.newBuilder()
                    .setRequestId(request.getRequestId() == null ? "" : request.getRequestId())
                    .setScene(request.getScene() == null ? "" : request.getScene())
                    .setUserId(request.getUserId() == null ? "" : request.getUserId())
                    .setDeviceId(request.getDeviceId() == null ? "" : request.getDeviceId())
                    .setIp(request.getIp() == null ? "" : request.getIp());

            if (request.getExtData() != null) {
                builder.putAllExtData(request.getExtData());
            }

            GrpcRiskResponse grpcResponse = stub.check(builder.build());

            return RiskResponse.builder()
                    .requestId(grpcResponse.getRequestId())
                    .action(grpcResponse.getAction())
                    .score(grpcResponse.getScore())
                    .reasonCodes(grpcResponse.getReasonCodesList())
                    .extData(new HashMap<>(grpcResponse.getExtDataMap()))
                    .message(grpcResponse.getMessage())
                    .build();
        } catch (StatusRuntimeException e) {
            log.error("[RiskGrpcClient] gRPC call failed, requestId={}, status={}",
                    request.getRequestId(), e.getStatus(), e);
            return RiskResponse.failOpen(
                    request.getRequestId() == null ? "unknown" : request.getRequestId(),
                    "GRPC_ERROR:" + e.getStatus().getCode().name());
        } finally {
            if (channel != channelManager.getChannel()) {
                channel.shutdown();
            }
        }
    }

    /**
     * Groovy 脚本测试（gRPC 版本）。
     */
    public GroovyTestResult testGroovyScript(String scriptContent, String testCaseName, Map<String, String> inputContext) {
        String traceId = MDC.get("traceId");
        ManagedChannel channel = channelManager.getChannelWithHeaders(traceId, null);
        try {
            RiskCheckServiceGrpc.RiskCheckServiceBlockingStub stub =
                    RiskCheckServiceGrpc.newBlockingStub(channel)
                            .withDeadlineAfter(5, TimeUnit.SECONDS);

            GrpcGroovyTestRequest.Builder builder = GrpcGroovyTestRequest.newBuilder()
                    .setScriptContent(scriptContent)
                    .setTestCaseName(testCaseName);
            if (inputContext != null) {
                builder.putAllInputContext(inputContext);
            }

            GrpcGroovyTestResult result = stub.testGroovyScript(builder.build());
            Long execMs = result.getExecutionTimeMs();
            return new GroovyTestResult(
                    result.getTestCaseName(),
                    result.getPassed(),
                    null,
                    null,
                    execMs,
                    result.getErrorMessage() == null ? null : result.getErrorMessage(),
                    null
            );
        } catch (StatusRuntimeException e) {
            log.error("[RiskGrpcClient] testGroovyScript failed, status={}", e.getStatus(), e);
            return new GroovyTestResult(
                    testCaseName,
                    false,
                    null,
                    null,
                    0L,
                    "GRPC_ERROR:" + e.getStatus().getCode().name(),
                    null
            );
        } finally {
            if (channel != channelManager.getChannel()) {
                channel.shutdown();
            }
        }
    }
}