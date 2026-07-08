package com.kato.pro.risk.client.grpc;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Metadata;
import io.grpc.stub.MetadataUtils;
import io.grpc.netty.shaded.io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.File;
import java.util.concurrent.TimeUnit;

/**
 * gRPC Channel 管理。
 * 单例持有 ManagedChannel，支持 header 注入（trace_id / tenant_id）。
 */
@Slf4j
@Component
public class GrpcChannelManager {

    @Value("${risk.grpc.host:localhost}")
    private String host;

    @Value("${risk.grpc.port:50051}")
    private int port;

    @Value("${risk.grpc.tls.enabled:false}")
    private boolean tlsEnabled;

    @Value("${risk.grpc.tls.ca.cert.path:}")
    private String caCertPath;

    private ManagedChannel channel;
    private io.grpc.netty.shaded.io.netty.handler.ssl.SslContext sslContext;

    @PostConstruct
    public void init() {
        if (tlsEnabled) {
            try {
                sslContext = GrpcSslContexts.forClient()
                        .trustManager(new File(caCertPath))
                        .build();
                channel = NettyChannelBuilder.forAddress(host, port)
                        .sslContext(sslContext)
                        .build();
                log.info("[GrpcChannel] Connected to {}:{} (TLS)", host, port);
            } catch (Exception e) {
                log.warn("[GrpcChannel] TLS channel creation failed, fallback to plaintext, host={}", host, e);
                sslContext = null;
                channel = ManagedChannelBuilder.forAddress(host, port)
                        .usePlaintext()
                        .build();
            }
        } else {
            sslContext = null;
            channel = ManagedChannelBuilder.forAddress(host, port)
                    .usePlaintext()
                    .build();
            log.info("[GrpcChannel] Connected to {}:{} (plaintext)", host, port);
        }
    }

    public ManagedChannel getChannel() {
        return channel;
    }

    public ManagedChannel getChannelWithHeaders(String traceId, String tenantId) {
        Metadata metadata = new Metadata();
        if (traceId != null) {
            metadata.put(Metadata.Key.of("x-trace-id", Metadata.ASCII_STRING_MARSHALLER), traceId);
        }
        if (tenantId != null) {
            metadata.put(Metadata.Key.of("x-tenant-id", Metadata.ASCII_STRING_MARSHALLER), tenantId);
        }
        if (tlsEnabled && sslContext != null) {
            return NettyChannelBuilder.forAddress(host, port)
                    .sslContext(sslContext)
                    .intercept(MetadataUtils.newAttachHeadersInterceptor(metadata))
                    .build();
        } else {
            return ManagedChannelBuilder.forAddress(host, port)
                    .usePlaintext()
                    .intercept(MetadataUtils.newAttachHeadersInterceptor(metadata))
                    .build();
        }
    }

    @PreDestroy
    public void shutdown() {
        try {
            channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
            log.info("[GrpcChannel] Shutdown completed");
        } catch (InterruptedException e) {
            log.warn("[GrpcChannel] Shutdown interrupted", e);
        }
    }
}