package com.kato.pro.resilience4j.config;

import com.kato.pro.resilience4j.KatoResilienceImportMarkers;
import com.kato.pro.resilience4j.KatoResilienceInstanceNames;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

/**
 * 在启用 {@link com.kato.pro.resilience4j.EnableKatoResilience4j} 且包含
 * {@link com.kato.pro.resilience4j.KatoResilienceTool#RETRY} 时由 {@link com.kato.pro.resilience4j.KatoResilienceImportSelector} 导入。
 * <p>
 * 类级别条件：标记 Bean {@link KatoResilienceImportMarkers#BEAN_NAME_RETRY}；无 {@code Enable} 时仅被扫描则不会生效。
 * <p>
 * 对 Kato 两套实例注册 {@code onError}、{@code onIgnoredError}，日志级别为 info。
 */
@Slf4j
@Configuration(proxyBeanMethods = false)
@ConditionalOnBean(name = KatoResilienceImportMarkers.BEAN_NAME_RETRY)
@AutoConfigureOrder(Ordered.LOWEST_PRECEDENCE)
public class KatoRetryInstanceConfiguration {

    @Bean(name = KatoResilienceInstanceNames.Retry.BEAN_DEFAULT)
    @ConditionalOnBean(RetryRegistry.class)
    public Retry katoRetryDefault(RetryRegistry registry) {
        String instanceName = KatoResilienceInstanceNames.Retry.INSTANCE_DEFAULT;
        Retry retry = registry.retry(instanceName);
        attachExceptionListeners(instanceName, registry);
        return retry;
    }

    @Bean(name = KatoResilienceInstanceNames.Retry.BEAN_ALT)
    @ConditionalOnBean(RetryRegistry.class)
    public Retry katoRetryAlt(RetryRegistry registry) {
        String instanceName = KatoResilienceInstanceNames.Retry.INSTANCE_ALT;
        Retry retry = registry.retry(instanceName);
        attachExceptionListeners(instanceName, registry);
        return retry;
    }

    private void attachExceptionListeners(String instance, RetryRegistry registry) {
        Retry retry = registry.retry(instance);
        retry.getEventPublisher()
                .onError(ev -> {
                    Throwable t = ev.getLastThrowable();
                    log.info("retry onError, instance={}, attempts={}", instance, ev.getNumberOfRetryAttempts(), t);
                })
                .onIgnoredError(ev -> {
                    Throwable t = ev.getLastThrowable();
                    log.info("retry onIgnoredError, instance={}", instance, t);
                });
    }
}
