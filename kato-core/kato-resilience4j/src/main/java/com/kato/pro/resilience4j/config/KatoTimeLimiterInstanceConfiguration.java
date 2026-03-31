package com.kato.pro.resilience4j.config;

import com.kato.pro.resilience4j.KatoResilienceImportMarkers;
import com.kato.pro.resilience4j.KatoResilienceInstanceNames;
import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

/**
 * 在启用 {@link com.kato.pro.resilience4j.EnableKatoResilience4j} 且包含
 * {@link com.kato.pro.resilience4j.KatoResilienceTool#TIME_LIMITER} 时导入。
 * <p>
 * 类级别条件：标记 Bean {@link KatoResilienceImportMarkers#BEAN_NAME_TIME_LIMITER}；无 {@code Enable} 时仅被扫描则不会生效。
 * <p>
 * 对 Kato 两套实例注册 {@code onError}、{@code onTimeout}，日志级别为 info。
 */
@Slf4j
@Configuration(proxyBeanMethods = false)
@ConditionalOnBean(name = KatoResilienceImportMarkers.BEAN_NAME_TIME_LIMITER)
@AutoConfigureOrder(Ordered.LOWEST_PRECEDENCE)
public class KatoTimeLimiterInstanceConfiguration {

    @Bean(name = KatoResilienceInstanceNames.TimeLimiter.BEAN_DEFAULT)
    @ConditionalOnBean(TimeLimiterRegistry.class)
    public TimeLimiter katoTimeLimiterDefault(TimeLimiterRegistry registry) {
        String instanceName = KatoResilienceInstanceNames.TimeLimiter.INSTANCE_DEFAULT;
        TimeLimiter timeLimiter = registry.timeLimiter(instanceName);
        attachExceptionListeners(instanceName, registry);
        return timeLimiter;
    }

    @Bean(name = KatoResilienceInstanceNames.TimeLimiter.BEAN_ALT)
    @ConditionalOnBean(TimeLimiterRegistry.class)
    public TimeLimiter katoTimeLimiterAlt(TimeLimiterRegistry registry) {
        String instanceName = KatoResilienceInstanceNames.TimeLimiter.INSTANCE_ALT;
        TimeLimiter timeLimiter = registry.timeLimiter(instanceName);
        attachExceptionListeners(instanceName, registry);
        return timeLimiter;
    }

    private void attachExceptionListeners(String instance, TimeLimiterRegistry registry) {
        TimeLimiter timeLimiter = registry.timeLimiter(instance);
        timeLimiter.getEventPublisher()
                .onError(ev -> {
                    Throwable t = ev.getThrowable();
                    log.info("timeLimiter onError, instance={}", instance, t);
                })
                .onTimeout(ev -> log.info("timeLimiter onTimeout, instance={}, event={}", instance, ev));
    }
}
