package com.kato.pro.resilience4j.config;

import com.kato.pro.resilience4j.KatoResilienceImportMarkers;
import com.kato.pro.resilience4j.KatoResilienceInstanceNames;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

/**
 * 在启用 {@link com.kato.pro.resilience4j.EnableKatoResilience4j} 且包含
 * {@link com.kato.pro.resilience4j.KatoResilienceTool#RATE_LIMITER} 时导入。
 * <p>
 * 类级别条件：标记 Bean {@link KatoResilienceImportMarkers#BEAN_NAME_RATE_LIMITER}；无 {@code Enable} 时仅被扫描则不会生效。
 * <p>
 * 对 Kato 两套实例注册 {@code onFailure}（获取许可失败等），日志级别为 info。
 */
@Slf4j
@Configuration(proxyBeanMethods = false)
@ConditionalOnBean(name = KatoResilienceImportMarkers.BEAN_NAME_RATE_LIMITER)
@AutoConfigureOrder(Ordered.LOWEST_PRECEDENCE)
public class KatoRateLimiterInstanceConfiguration {

    @Bean(name = KatoResilienceInstanceNames.RateLimiter.BEAN_DEFAULT)
    @ConditionalOnBean(RateLimiterRegistry.class)
    public RateLimiter katoRateLimiterDefault(RateLimiterRegistry registry) {
        String instanceName = KatoResilienceInstanceNames.RateLimiter.INSTANCE_DEFAULT;
        RateLimiter limiter = registry.rateLimiter(instanceName);
        attachFailureListeners(instanceName, registry);
        return limiter;
    }

    @Bean(name = KatoResilienceInstanceNames.RateLimiter.BEAN_ALT)
    @ConditionalOnBean(RateLimiterRegistry.class)
    public RateLimiter katoRateLimiterAlt(RateLimiterRegistry registry) {
        String instanceName = KatoResilienceInstanceNames.RateLimiter.INSTANCE_ALT;
        RateLimiter limiter = registry.rateLimiter(instanceName);
        attachFailureListeners(instanceName, registry);
        return limiter;
    }

    private void attachFailureListeners(String instance, RateLimiterRegistry registry) {
        RateLimiter limiter = registry.rateLimiter(instance);
        limiter.getEventPublisher()
                .onFailure(ev -> log.info("rateLimiter onFailure, instance={}, event={}", instance, ev));
    }
}
