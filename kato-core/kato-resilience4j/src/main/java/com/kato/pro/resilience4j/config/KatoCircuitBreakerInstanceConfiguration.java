package com.kato.pro.resilience4j.config;

import com.kato.pro.resilience4j.KatoResilienceImportMarkers;
import com.kato.pro.resilience4j.KatoResilienceInstanceNames;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

/**
 * 由 {@link com.kato.pro.resilience4j.KatoResilienceImportSelector} 在启用
 * {@link com.kato.pro.resilience4j.EnableKatoResilience4j} 且包含
 * {@link com.kato.pro.resilience4j.KatoResilienceTool#CIRCUIT_BREAKER} 时导入。
 * <p>
 * 类级别条件：标记 Bean {@link KatoResilienceImportMarkers#BEAN_NAME_CIRCUIT_BREAKER}；无 {@code Enable} 时仅被扫描则不会生效。
 * <p>
 * 对 Kato 两套实例注册异常相关事件：{@code onError}、{@code onIgnoredError}、{@code onCallNotPermitted}，日志级别为 info。
 */
@Slf4j
@Configuration(proxyBeanMethods = false)
@ConditionalOnBean(name = KatoResilienceImportMarkers.BEAN_NAME_CIRCUIT_BREAKER)
@AutoConfigureOrder(Ordered.LOWEST_PRECEDENCE)
public class KatoCircuitBreakerInstanceConfiguration {

    @Bean(name = KatoResilienceInstanceNames.CircuitBreaker.BEAN_DEFAULT)
    @ConditionalOnBean(CircuitBreakerRegistry.class)
    public CircuitBreaker katoCircuitBreakerDefault(CircuitBreakerRegistry registry) {
        String instanceName = KatoResilienceInstanceNames.CircuitBreaker.INSTANCE_DEFAULT;
        CircuitBreaker circuitBreaker = registry.circuitBreaker(instanceName);
        attachExceptionListeners(instanceName, registry);
        return circuitBreaker;
    }

    @Bean(name = KatoResilienceInstanceNames.CircuitBreaker.BEAN_ALT)
    @ConditionalOnBean(CircuitBreakerRegistry.class)
    public CircuitBreaker katoCircuitBreakerAlt(CircuitBreakerRegistry registry) {
        String instanceName = KatoResilienceInstanceNames.CircuitBreaker.INSTANCE_ALT;
        CircuitBreaker circuitBreaker = registry.circuitBreaker(instanceName);
        attachExceptionListeners(instanceName, registry);
        return circuitBreaker;
    }

    private void attachExceptionListeners(String instance, CircuitBreakerRegistry registry) {
        CircuitBreaker cb = registry.circuitBreaker(instance);
        cb.getEventPublisher()
                .onError(ev -> {
                    Throwable t = ev.getThrowable();
                    log.info("circuitBreaker onError, instance={}", instance, t);
                })
                .onIgnoredError(ev -> {
                    Throwable t = ev.getThrowable();
                    log.info("circuitBreaker onIgnoredError, instance={}", instance, t);
                })
                .onCallNotPermitted(ev ->
                        log.info("circuitBreaker onCallNotPermitted, instance={}, event={}", instance, ev));
    }
}
