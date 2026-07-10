package com.kato.pro.langchain.config;

import com.kato.pro.langchain.domain.safety.CompositeSafetyFilter;
import com.kato.pro.langchain.domain.safety.ContentSafetyFilter;
import com.kato.pro.langchain.domain.safety.HttpSensitiveSafetyFilter;
import com.kato.pro.langchain.domain.safety.LocalKeywordSafetyFilter;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

@Configuration
@EnableConfigurationProperties(SafetyProperties.class)
public class SafetyConfig {

    @Bean
    public HttpSensitiveSafetyFilter httpSensitiveSafetyFilter(SafetyProperties properties) {
        return new HttpSensitiveSafetyFilter(properties);
    }

    /**
     * Composite filter：始终包含 LocalKeyword；HttpSensitive 按 properties 决定是否加入。
     */
    @Bean
    public ContentSafetyFilter compositeSafetyFilter(
            LocalKeywordSafetyFilter local,
            HttpSensitiveSafetyFilter http,
            SafetyProperties properties) {
        List<ContentSafetyFilter> filters = new ArrayList<>();
        filters.add(local);
        if (properties.getSensitiveClient().isEnabled()) {
            filters.add(http);
        }
        return new CompositeSafetyFilter(filters);
    }
}
