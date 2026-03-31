package com.kato.pro.resilience4j;

/**
 * 与 {@link EnableKatoResilience4j} 配合使用，声明需要注册「显式 Bean」的 Resilience4j 能力种类。
 * <p>
 * 未在注解数组中出现的工具，不会导入对应的 Spring 配置类，因而不会声明该工具下的 {@code kato*Default}/{@code kato*Alt} Bean。
 */
public enum KatoResilienceTool {

    /**
     * 断路器：失败/慢调用达到一定阈值后短时间拒绝调用，用于防止下游故障拖垮调用方（雪崩）。
     */
    CIRCUIT_BREAKER,

    /**
     * 重试：在符合条件的异常上对调用进行有限次重试（可配置间隔与退避），用于吸收瞬时故障。
     */
    RETRY,

    /**
     * 限流：限制在周期内的最大调用次数，用于保护下游配额与本机资源。
     */
    RATE_LIMITER,

    /**
     * 舱壁：限制并发调用数量（或线程池语义），用于把不同依赖隔离，避免慢调用占满线程。
     */
    BULKHEAD,

    /**
     * 超时：为异步 {@link java.util.concurrent.CompletionStage} 设置执行时限，超时则失败或走降级。
     */
    TIME_LIMITER
}
