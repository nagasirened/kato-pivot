package com.kato.pro.oss.config;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.kato.pro.oss.core.DefaultOssTemplate;
import com.kato.pro.oss.core.OssClientFactory;
import com.kato.pro.oss.core.OssLinksPoolUtils;
import com.kato.pro.oss.core.OssProperties;
import com.kato.pro.oss.core.OssTemplate;
import org.springframework.beans.factory.annotation.Autowired;
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

    @Bean
    public OSS ossClient() {
        return new OSSClientBuilder().build(ossProperties.getEndpoint(), ossProperties.getAccessKeyId(), ossProperties.getAccessKeySecret());
    }

    @Bean
    public OssTemplate ossTemplate(@Autowired OSS ossClient) {
        return new DefaultOssTemplate(ossClient);
    }
}