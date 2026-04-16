package com.kato.pro.oss.config;

import com.kato.pro.oss.core.*;
import com.kato.pro.oss.repository.OssRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(OssProperties.class)
@ConditionalOnProperty(prefix = "kato.oss", name = "enable", havingValue = "true", matchIfMissing = false)
public class OssConfiguration {

    @Autowired
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

    @Bean
    @ConditionalOnBean(OssLinksPoolUtils.class)
    public OssTemplate ossTemplate(OssLinksPoolUtils ossLinksPoolUtils) {
        return new DefaultOssTemplate(ossLinksPoolUtils);
    }

    @Bean
    public OssRepository ossRepository() {
        return new OssRepository(ossProperties);
    }

}