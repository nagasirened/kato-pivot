package com.kato.pro.config;


import com.kato.pro.client.KatoClientProcessor;
import com.kato.pro.constant.ServiceType;
import com.kato.pro.discovery.CuratorDiscoveryService;
import com.kato.pro.discovery.DiscoveryService;
import com.kato.pro.discovery.RedissonDiscoveryService;
import com.kato.pro.loadbalance.LoadBalancer;
import com.kato.pro.loadbalance.RandomBalancer;
import com.kato.pro.loadbalance.RoundRobinBalancer;
import jodd.util.StringUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Objects;

@Configuration
@EnableConfigurationProperties({KatoClientProperties.class})
public class KatoRpcClientConfig {

    @Autowired
    private KatoClientProperties katoClientProperties;

    @Bean
    @ConditionalOnMissingBean
    public LoadBalancer loadBalancer() {
        String balance = katoClientProperties.getBalance();
        if (StringUtil.equals(balance, "random")) {
            return new RandomBalancer();
        } else if (StringUtil.equals(balance, "robin")) {
            return new RoundRobinBalancer();
        }
        return new RoundRobinBalancer();
    }

    @Bean
    @ConditionalOnMissingBean
    public DiscoveryService discoveryService() {
        ServiceType serviceType = this.katoClientProperties.getServiceType();
        if (Objects.requireNonNull(serviceType) == ServiceType.REDIS) {
            return new RedissonDiscoveryService(katoClientProperties.getDiscoverAddress(), loadBalancer());
        }
        return new CuratorDiscoveryService(katoClientProperties.getDiscoverAddress(), loadBalancer());
    }

    @Bean
    @ConditionalOnMissingBean
    public KatoClientProcessor katoClientProcessor() {
        return new KatoClientProcessor(discoveryService(), katoClientProperties);
    }


}
