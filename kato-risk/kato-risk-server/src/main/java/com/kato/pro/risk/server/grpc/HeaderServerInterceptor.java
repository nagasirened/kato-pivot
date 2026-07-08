package com.kato.pro.risk.server.grpc;

import io.grpc.*;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;

/**
 * gRPC Header 拦截器。
 * 提取 trace_id / tenant_id 等元数据到 MDC，方便日志串联。
 */
@Slf4j
public class HeaderServerInterceptor implements ServerInterceptor {

    private static final Metadata.Key<String> TRACE_ID_KEY =
            Metadata.Key.of("x-trace-id", Metadata.ASCII_STRING_MARSHALLER);
    private static final Metadata.Key<String> TENANT_ID_KEY =
            Metadata.Key.of("x-tenant-id", Metadata.ASCII_STRING_MARSHALLER);

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> call,
            Metadata headers,
            ServerCallHandler<ReqT, RespT> next) {

        String traceId = headers.get(TRACE_ID_KEY);
        String tenantId = headers.get(TENANT_ID_KEY);

        if (traceId != null) {
            MDC.put("traceId", traceId);
        }
        if (tenantId != null) {
            MDC.put("tenantId", tenantId);
        }

        log.debug("[gRPC Header] traceId={}, tenantId={}", traceId, tenantId);

        return next.startCall(call, headers);
    }
}