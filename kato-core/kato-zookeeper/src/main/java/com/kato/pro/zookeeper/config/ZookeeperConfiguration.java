package com.kato.pro.zookeeper.config;

import com.kato.pro.zookeeper.config.property.CuratorProperties;
import com.kato.pro.zookeeper.config.property.ZookeeperProperties;
import com.kato.pro.zookeeper.lock.ZookeeperSession;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(ZookeeperProperties.class)
public class ZookeeperConfiguration {

    @Bean
    @ConditionalOnProperty(value = ZookeeperProperties.PREFIX + ZookeeperProperties.Fields.basic + ".enabled", havingValue = "true")
    public ZookeeperSession zookeeperSession(ZookeeperProperties zookeeperProperties) throws Exception {
        return new ZookeeperSession(zookeeperProperties.getBasic());
    }

    @Bean
    @ConditionalOnProperty(value = ZookeeperProperties.PREFIX + ZookeeperProperties.Fields.curator + ".enabled", havingValue = "true")
    public CuratorFramework curatorFrameworkFactory(ZookeeperProperties zookeeperProperties) throws Exception {
        CuratorProperties curatorProperties = zookeeperProperties.getCurator();
        ExponentialBackoffRetry retryPolicy = new ExponentialBackoffRetry(1000, curatorProperties.getMaxRetries());
        return CuratorFrameworkFactory.builder()
                .connectString(curatorProperties.getAddress())
                .sessionTimeoutMs(curatorProperties.getSessionTimeout())
                .connectionTimeoutMs(curatorProperties.getConnectionTimeout())
                .retryPolicy(retryPolicy)
                .build();
    }

}



