package com.kato.pro.resilience4j;

import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.type.AnnotationMetadata;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * 在 {@link KatoResilienceImportSelector} 解析配置类<strong>之前</strong>注册各工具的标记 Bean 定义，
 * 供 {@code Kato*InstanceConfiguration} 上的 {@code ConditionalOnBean(name=...)} 判定。
 *
 * @see KatoResilienceImportMarkers
 */
public class KatoResilienceToolMarkersRegistrar implements ImportBeanDefinitionRegistrar {

    @Override
    public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
        if (!importingClassMetadata.hasAnnotation(EnableKatoResilience4j.class.getName())) {
            return;
        }
        Map<String, Object> attrs = importingClassMetadata.getAnnotationAttributes(EnableKatoResilience4j.class.getName(), true);
        if (attrs == null) {
            return;
        }
        KatoResilienceTool[] tools = (KatoResilienceTool[]) attrs.get("value");
        if (tools == null || tools.length == 0) {
            return;
        }
        Set<KatoResilienceTool> unique = EnumSet.noneOf(KatoResilienceTool.class);
        for (KatoResilienceTool t : tools) {
            if (t != null) {
                unique.add(t);
            }
        }
        for (KatoResilienceTool tool : unique) {
            registerMarker(registry, tool);
        }
    }

    private static void registerMarker(BeanDefinitionRegistry registry, KatoResilienceTool tool) {
        String beanName = KatoResilienceImportMarkers.beanName(tool);
        if (registry.containsBeanDefinition(beanName)) {
            return;
        }
        RootBeanDefinition def = new RootBeanDefinition(KatoResilienceImportMarker.class);
        registry.registerBeanDefinition(beanName, def);
    }
}
