package com.kato.pro.config;


import com.kato.pro.register.CuratorRegisterService;
import com.kato.pro.register.RedissonRegisterService;
import com.kato.pro.register.RegisterService;
import com.kato.pro.server.NettyServer;
import com.kato.pro.server.RpcServer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.Resource;

@Configuration
@EnableConfigurationProperties(KatoRpcProperties.class)
public class KatoRpcServerConfig {

    @Resource
    private KatoRpcProperties properties;

    @Bean
    @ConditionalOnMissingBean(RegisterService.class)
    public RegisterService registerService() {
        String serverAddress = properties.getServerAddress();
        switch (properties.getServiceType()) {
            case REDIS:
                return new RedissonRegisterService(serverAddress);
            default:
                return new CuratorRegisterService(serverAddress);
        }
    }

    @Bean
    @ConditionalOnMissingBean(RpcServer.class)
    public RpcServer rpcServer() {
        return new NettyServer();
    }
}
