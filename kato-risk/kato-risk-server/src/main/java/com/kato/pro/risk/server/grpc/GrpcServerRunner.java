package com.kato.pro.risk.server.grpc;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.ServerInterceptors;
import io.grpc.netty.shaded.io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder;
import io.grpc.netty.shaded.io.netty.handler.ssl.SslContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;

/**
 * gRPC Server 启动器。
 * 在 Spring 容器启动完成后拉起 gRPC 端口，注册 RiskCheckServiceImpl。
 */
@Slf4j
@Component
public class GrpcServerRunner implements ApplicationListener<ContextRefreshedEvent> {

    @Value("${risk.grpc.tls.enabled:false}")
    private boolean tlsEnabled;

    @Value("${risk.grpc.tls.cert.path:}")
    private String certPath;

    @Value("${risk.grpc.tls.private.key.path:}")
    private String privateKeyPath;

    @Value("${risk.grpc.port:50051}")
    private int port;

    private Server server;

    @Autowired
    private RiskCheckServiceImpl riskCheckService;

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        try {
            if (tlsEnabled) {
                SslContext sslContext = GrpcSslContexts.forServer(
                        new File(certPath),
                        new File(privateKeyPath))
                        .build();

                server = NettyServerBuilder.forPort(port)
                        .sslContext(sslContext)
                        .addService(ServerInterceptors.intercept(
                                riskCheckService,
                                new HeaderServerInterceptor()))
                        .build()
                        .start();
                log.info("[GrpcServer] gRPC Server started with TLS, port={}", port);
            } else {
                server = ServerBuilder.forPort(port)
                        .addService(ServerInterceptors.intercept(
                                riskCheckService,
                                new HeaderServerInterceptor()))
                        .build()
                        .start();
                log.info("[GrpcServer] gRPC Server started (plaintext), port={}", port);
            }

            // 优雅停机
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                log.info("[GrpcServer] Shutting down gRPC server...");
                if (server != null) {
                    server.shutdown();
                }
            }));
        } catch (IOException e) {
            log.error("[GrpcServer] Failed to start gRPC server, port={}", port, e);
        }
    }

    public Server getServer() {
        return server;
    }
}