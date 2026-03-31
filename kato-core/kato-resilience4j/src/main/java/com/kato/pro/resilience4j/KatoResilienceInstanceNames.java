package com.kato.pro.resilience4j;

/**
 * 内置 YAML 中 {@code resilience4j.*.instances} 下的实例名，以及与 {@link org.springframework.beans.factory.annotation.Qualifier}
 * 配合使用的 Spring Bean 名称。
 * <p>
 * 仅当在 {@link EnableKatoResilience4j} 中启用了对应 {@link KatoResilienceTool}、且已注册
 * {@link KatoResilienceImportMarkers} 所列标记 Bean 时，相关 Bean 才会被注册（避免仅组件扫描命中配置类时误注册）。
 */
public final class KatoResilienceInstanceNames {

    private KatoResilienceInstanceNames() {
    }

    /** 断路器：默认 / 偏严格两套实例与 Bean 名。 */
    public static final class CircuitBreaker {
        public static final String INSTANCE_DEFAULT = "kato-circuitbreaker-default";
        public static final String INSTANCE_ALT = "kato-circuitbreaker-alt";
        public static final String BEAN_DEFAULT = "katoCircuitBreakerDefault";
        public static final String BEAN_ALT = "katoCircuitBreakerAlt";
    }

    /** 重试：默认 / 偏激进两套实例与 Bean 名。 */
    public static final class Retry {
        public static final String INSTANCE_DEFAULT = "kato-retry-default";
        public static final String INSTANCE_ALT = "kato-retry-alt";
        public static final String BEAN_DEFAULT = "katoRetryDefault";
        public static final String BEAN_ALT = "katoRetryAlt";
    }

    /** 限流：默认 / 更高配额两套实例与 Bean 名。 */
    public static final class RateLimiter {
        public static final String INSTANCE_DEFAULT = "kato-ratelimiter-default";
        public static final String INSTANCE_ALT = "kato-ratelimiter-alt";
        public static final String BEAN_DEFAULT = "katoRateLimiterDefault";
        public static final String BEAN_ALT = "katoRateLimiterAlt";
    }

    /** 舱壁：默认 / 更小并发两套实例与 Bean 名。 */
    public static final class Bulkhead {
        public static final String INSTANCE_DEFAULT = "kato-bulkhead-default";
        public static final String INSTANCE_ALT = "kato-bulkhead-alt";
        public static final String BEAN_DEFAULT = "katoBulkheadDefault";
        public static final String BEAN_ALT = "katoBulkheadAlt";
    }

    /** 超时：默认较宽 / 较紧两套实例与 Bean 名。 */
    public static final class TimeLimiter {
        public static final String INSTANCE_DEFAULT = "kato-timelimiter-default";
        public static final String INSTANCE_ALT = "kato-timelimiter-alt";
        public static final String BEAN_DEFAULT = "katoTimeLimiterDefault";
        public static final String BEAN_ALT = "katoTimeLimiterAlt";
    }
}
