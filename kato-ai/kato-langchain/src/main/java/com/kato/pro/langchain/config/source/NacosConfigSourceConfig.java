package com.kato.pro.langchain.config.source;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * NacosConfigSource 启用配置。
 *
 * 触发条件：kato.config.nacos.enabled=true（沙箱默认 false → 走 LocalConfigSource）。
 *
 * dataId 列表通过 kato.config.nacos.data-ids 注入（yaml 数组），NacosConfigProperties 承载。
 */
@Slf4j
@Configuration
@ConditionalOnProperty(name = "kato.config.nacos.enabled", havingValue = "true")
@EnableConfigurationProperties(NacosConfigProperties.class)
public class NacosConfigSourceConfig {

    private NacosConfigSource nacosConfigSource;

    @Bean
    public NacosConfigSource nacosConfigSource(NacosConfigProperties properties) {
        log.info("Creating NacosConfigSource: server={} namespace={} dataIds={}",
                properties.getServerAddr(), properties.getNamespace(), properties.getDataIds());
        this.nacosConfigSource = new NacosConfigSource(properties);
        return this.nacosConfigSource;
    }

    @PreDestroy
    public void close() {
        if (nacosConfigSource != null) {
            log.info("Closing NacosConfigSource");
            nacosConfigSource.close();
        }
    }
}
