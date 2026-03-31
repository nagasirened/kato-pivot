package com.kato.pro.resilience4j.config;

import com.kato.pro.resilience4j.KatoResilienceImportMarkers;
import com.kato.pro.resilience4j.KatoResilienceInstanceNames;
import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

/**
 * 在启用 {@link com.kato.pro.resilience4j.EnableKatoResilience4j} 且包含
 * {@link com.kato.pro.resilience4j.KatoResilienceTool#BULKHEAD} 时导入。
 * <p>
 * 类级别条件依赖标记 Bean {@link KatoResilienceImportMarkers#BEAN_NAME_BULKHEAD}；无 {@code Enable} 时仅被扫描则不会生效。
 * <p>
 * 对 Kato 两套实例注册 {@code onCallRejected}（并发已满或被拒绝），日志级别为 info。
 */
@Slf4j
@Configuration(proxyBeanMethods = false)
@ConditionalOnBean(name = KatoResilienceImportMarkers.BEAN_NAME_BULKHEAD)
@AutoConfigureOrder(Ordered.LOWEST_PRECEDENCE)
public class KatoBulkheadInstanceConfiguration {

    @Bean(name = KatoResilienceInstanceNames.Bulkhead.BEAN_DEFAULT)
    @ConditionalOnBean(BulkheadRegistry.class)
    public Bulkhead katoBulkheadDefault(BulkheadRegistry registry) {
        String instanceName = KatoResilienceInstanceNames.Bulkhead.INSTANCE_DEFAULT;
        Bulkhead bulkhead = registry.bulkhead(instanceName);
        attachRejectionListeners(instanceName, registry);
        return bulkhead;
    }

    @Bean(name = KatoResilienceInstanceNames.Bulkhead.BEAN_ALT)
    @ConditionalOnBean(BulkheadRegistry.class)
    public Bulkhead katoBulkheadAlt(BulkheadRegistry registry) {
        String instanceName = KatoResilienceInstanceNames.Bulkhead.INSTANCE_ALT;
        Bulkhead bulkhead = registry.bulkhead(instanceName);
        attachRejectionListeners(instanceName, registry);
        return bulkhead;
    }

    private void attachRejectionListeners(String instance, BulkheadRegistry registry) {
        Bulkhead bulkhead = registry.bulkhead(instance);
        bulkhead.getEventPublisher()
                .onCallRejected(ev -> log.info("bulkhead onCallRejected, instance={}, event={}", instance, ev));
    }

}
