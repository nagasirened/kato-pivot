package com.kato.pro.resilience4j;

/**
 * 由 {@link KatoResilienceToolMarkersRegistrar} 在解析 {@link EnableKatoResilience4j} 时注册的占位 Bean 名称。
 * <p>
 * 各 {@code Kato*InstanceConfiguration} 通过类级别 {@link org.springframework.boot.autoconfigure.condition.ConditionalOnBean}
 * 依赖对应名称；仅组件扫描、未经过 {@code Enable} 时不会存在这些定义，从而不会注册本模块的 Resilience 实例 Bean。
 * <p>
 * {@code BEAN_NAME_*} 必须使用<strong>字符串字面量</strong>赋值，以满足注解属性「编译期常量」要求；须与
 * {@link #beanName(KatoResilienceTool)} 返回值保持一致（规则：{@code katoResilience.import.marker.} + {@link Enum#name()}）。
 */
public final class KatoResilienceImportMarkers {

    private KatoResilienceImportMarkers() {
    }

    public static final String BEAN_NAME_CIRCUIT_BREAKER = "katoResilience.import.marker.CIRCUIT_BREAKER";
    public static final String BEAN_NAME_RETRY = "katoResilience.import.marker.RETRY";
    public static final String BEAN_NAME_RATE_LIMITER = "katoResilience.import.marker.RATE_LIMITER";
    public static final String BEAN_NAME_BULKHEAD = "katoResilience.import.marker.BULKHEAD";
    public static final String BEAN_NAME_TIME_LIMITER = "katoResilience.import.marker.TIME_LIMITER";

    /**
     * 与 {@link KatoResilienceToolMarkersRegistrar} 使用，须与各 {@link #BEAN_NAME_CIRCUIT_BREAKER} 等字面量一致。
     */
    public static String beanName(KatoResilienceTool tool) {
        return "katoResilience.import.marker." + tool.name();
    }
}
