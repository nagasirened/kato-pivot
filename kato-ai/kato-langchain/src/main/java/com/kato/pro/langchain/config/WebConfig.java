package com.kato.pro.langchain.config;

import com.kato.pro.langchain.common.tenant.TenantContextFilter;
import com.kato.pro.langchain.common.trace.TraceIdFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

/**
 * Web 层配置：集中注册过滤器 + 设置显式顺序。
 *
 * 顺序（从低到高 = 从先到后执行）：
 *   1. TraceIdFilter         (HIGHEST_PRECEDENCE)
 *   2. TenantContextFilter   (HIGHEST_PRECEDENCE + 10)
 *
 * 后续模块会在这里追加：CORS、RateLimit、RequestLogging、Auth 等过滤器。
 */
@Configuration
public class WebConfig {

    @Bean
    public FilterRegistrationBean<TraceIdFilter> traceIdFilterRegistration(TraceIdFilter filter) {
        FilterRegistrationBean<TraceIdFilter> reg = new FilterRegistrationBean<>(filter);
        reg.setOrder(Ordered.HIGHEST_PRECEDENCE);
        reg.addUrlPatterns("/*");
        reg.setName("traceIdFilter");
        return reg;
    }

    @Bean
    public FilterRegistrationBean<TenantContextFilter> tenantContextFilterRegistration(TenantContextFilter filter) {
        FilterRegistrationBean<TenantContextFilter> reg = new FilterRegistrationBean<>(filter);
        reg.setOrder(Ordered.HIGHEST_PRECEDENCE + 10);
        reg.addUrlPatterns("/*");
        reg.setName("tenantContextFilter");
        return reg;
    }
}
