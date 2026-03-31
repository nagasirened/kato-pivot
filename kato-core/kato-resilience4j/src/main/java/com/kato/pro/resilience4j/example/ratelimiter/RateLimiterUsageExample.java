package com.kato.pro.resilience4j.example.ratelimiter;

import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.resilience4j.ratelimiter.RateLimiter;

/**
 * <p><b>轻量级限（Rate Limiter）</b>在固定时间周期内限制调用次数，超出部分可阻塞等待、快速失败等（取决于配置），
 * 用于保护下游配额、平滑流量、防止突发把弱依赖打满。</p>
 *
 * <p><b>典型效果：</b>每个刷新周期开始时恢复可用额度；当周期内已用尽额度，新请求可能被拒绝或排队。
 * 与「并发舱壁」不同：限流主要约束的是「单位时间内的调用次数」，而非「同时进行多少调用」。</p>
 *
 * <p><b>本类示范两种编程式用法：</b></p>
 * <ul>
 *   <li>{@link #demonstrateExecute}: 通过 {@link io.github.resilience4j.ratelimiter.RateLimiter#executeSupplier} 在限流规则下立即执行。</li>
 *   <li>{@link #demonstrateDecorate}: 通过 {@link io.github.resilience4j.ratelimiter.RateLimiter#decorateSupplier} 包装为
 *       {@link Supplier}，便于复用或组合。</li>
 * </ul>
 */
public final class RateLimiterUsageExample {

    private static final Logger log = LoggerFactory.getLogger(RateLimiterUsageExample.class);

    private RateLimiterUsageExample() {
    }

    /** 在限流器控制下执行一次示例调用。 */
    public static String demonstrateExecute(RateLimiter rateLimiter) {
        return rateLimiter.executeSupplier(RateLimiterUsageExample::guardedCall);
    }

    /** 返回包装后的 {@link Supplier}，实际消耗额度发生在 {@code get()} 时。 */
    public static String demonstrateDecorate(RateLimiter rateLimiter) {
        Supplier<String> decorated =
                RateLimiter.decorateSupplier(rateLimiter, RateLimiterUsageExample::guardedCall);
        try {
            return decorated.get();
        } catch (RuntimeException e) {
            log.warn("rateLimiter decorate path failed, name={}", rateLimiter.getName(), e);
            throw e;
        }
    }

    private static String guardedCall() {
        return "ok";
    }
}
