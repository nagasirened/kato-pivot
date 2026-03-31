package com.kato.pro.resilience4j.example.retry;

import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.resilience4j.retry.Retry;

/**
 * <p><b>重试（Retry）</b>在调用因瞬时错误（网络抖动、短暂 5xx 等）失败时，按策略进行有限次重试，
 * 提高调用成功率；需配合合适的异常类型、最大次数与间隔，避免放大故障或压垮下游。</p>
 *
 * <p><b>典型效果：</b>单次调用失败后，在配置的最大尝试次数内等待（可选固定间隔或指数退避）再次执行；
 * 超过次数仍失败则把异常抛给调用方。对「幂等」或「可安全重复」的操作更合适用。</p>
 *
 * <p><b>本类示范两种编程式用法：</b></p>
 * <ul>
 *   <li>{@link #demonstrateExecute}: 一次性在重试上下文中执行 {@link io.github.resilience4j.retry.Retry#executeSupplier}。</li>
 *   <li>{@link #demonstrateDecorate}: 使用 {@link io.github.resilience4j.retry.Retry#decorateSupplier} 得到可重复的
 *       {@link Supplier}，便于延迟执行或传递。</li>
 * </ul>
 */
public final class RetryUsageExample {

    private static final Logger log = LoggerFactory.getLogger(RetryUsageExample.class);

    private RetryUsageExample() {
    }

    /**
     * 按重试配置执行一次示例调用（内部会视失败情况自动重试直至成功或耗尽次数）。
     */
    public static String demonstrateExecute(Retry retry) {
        return retry.executeSupplier(RetryUsageExample::flakyCall);
    }

    /**
     * 将示例调用装饰为带重试策略的 {@link Supplier}，在 {@code get()} 时生效。
     */
    public static String demonstrateDecorate(Retry retry) {
        Supplier<String> decorated = Retry.decorateSupplier(retry, RetryUsageExample::flakyCall);
        try {
            return decorated.get();
        } catch (RuntimeException e) {
            log.warn("retry decorate path failed, name={}", retry.getName(), e);
            throw e;
        }
    }

    private static String flakyCall() {
        return "ok";
    }
}
