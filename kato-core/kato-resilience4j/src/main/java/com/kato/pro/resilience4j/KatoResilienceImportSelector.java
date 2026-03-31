package com.kato.pro.resilience4j;

import com.kato.pro.resilience4j.config.*;
import org.springframework.context.annotation.ImportSelector;
import org.springframework.core.type.AnnotationMetadata;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * 根据 {@link EnableKatoResilience4j#value()} 选择要导入的按工具拆分的配置类。
 * <p>
 * 须与 {@link KatoResilienceToolMarkersRegistrar} 同列于 {@link EnableKatoResilience4j#import} 中且在其后，
 * 以保证对应标记 Bean 定义已注册后再解析本选择器导入的配置类。
 */
public class KatoResilienceImportSelector implements ImportSelector {

    @Override
    public String[] selectImports(AnnotationMetadata importingClassMetadata) {
        if (!importingClassMetadata.hasAnnotation(EnableKatoResilience4j.class.getName())) {
            return new String[0];
        }
        Map<String, Object> attrs = importingClassMetadata.getAnnotationAttributes(EnableKatoResilience4j.class.getName(), true);
        if (attrs == null) {
            return new String[0];
        }
        KatoResilienceTool[] tools = (KatoResilienceTool[]) attrs.get("value");
        if (tools == null || tools.length == 0) {
            return new String[0];
        }
        Set<String> configurationClasses = getConfigurations(tools);
        return configurationClasses.toArray(new String[0]);
    }

    private static Set<String> getConfigurations(KatoResilienceTool[] tools) {
        Set<String> configurationClasses = new LinkedHashSet<>();
        for (KatoResilienceTool tool : tools) {
            if (tool == null) {
                continue;
            }
            switch (tool) {
                case CIRCUIT_BREAKER -> configurationClasses.add(KatoCircuitBreakerInstanceConfiguration.class.getName());
                case RETRY ->           configurationClasses.add(KatoRetryInstanceConfiguration.class.getName());
                case RATE_LIMITER ->    configurationClasses.add(KatoRateLimiterInstanceConfiguration.class.getName());
                case BULKHEAD ->        configurationClasses.add(KatoBulkheadInstanceConfiguration.class.getName());
                case TIME_LIMITER ->    configurationClasses.add(KatoTimeLimiterInstanceConfiguration.class.getName());
            }
        }
        return configurationClasses;
    }
}
