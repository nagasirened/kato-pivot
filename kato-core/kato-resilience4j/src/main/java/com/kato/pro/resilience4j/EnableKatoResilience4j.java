package com.kato.pro.resilience4j;

import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

/**
 * 启用 Kato 封装的一组 Resilience4j 显式 Bean（与 {@link KatoResilienceInstanceNames}、内置 YAML 默认配置配合）。
 * <p>
 * 仅在 {@link #value()} 数组中出现的工具才会加载对应配置类，从而注册该工具下预置名称的 {@code CircuitBreaker}/{@code Retry} 等 Bean。
 * 数组为空时不导入任何工具配置（不注册上述 Bean）。
 * <p>
 * 典型用法：标注在启动类或任意 {@code @Configuration} 上，且需保证 classpath 中存在 {@code resilience4j-spring-boot3} 及 AOP（若使用声明式注解）。
 * <p>
 * {@link KatoResilienceToolMarkersRegistrar} 先于 {@link KatoResilienceImportSelector} 执行，注册与各工具对应的标记 Bean，
 * 以便配置类上的条件在组件扫描误命中时仍不生效。
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import({KatoResilienceToolMarkersRegistrar.class, KatoResilienceImportSelector.class})
public @interface EnableKatoResilience4j {

    /**
     * 需要注册显式 Bean 的工具列表；每一项对应一种 Resilience4j 能力。
     *
     * @return 工具枚举数组；可省略重复项，重复项在导入时会被去重
     */
    KatoResilienceTool[] value() default {};
}
