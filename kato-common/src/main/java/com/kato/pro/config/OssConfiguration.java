package com.kato.pro.config;

import com.kato.pro.oss.OssClientFactory;
import com.kato.pro.oss.OssLinksPoolUtils;
import com.kato.pro.oss.OssProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.Resource;

@Configuration
@EnableConfigurationProperties(OssProperties.class)
@ConditionalOnProperty(prefix = "kato.oss", name = "enable", havingValue = "true", matchIfMissing = false)
public class OssConfiguration {

    @Resource
    private OssProperties ossProperties;

    @Bean
    public OssClientFactory ossClientFactory() {
        return new OssClientFactory(ossProperties);
    }

    @Bean
    @ConditionalOnBean(OssClientFactory.class)
    public OssLinksPoolUtils ossPool() {
        return new OssLinksPoolUtils(ossClientFactory());
    }

}
