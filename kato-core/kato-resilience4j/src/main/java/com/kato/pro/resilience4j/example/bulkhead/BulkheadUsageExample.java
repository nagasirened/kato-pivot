package com.kato.pro.resilience4j.example.bulkhead;

import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.resilience4j.bulkhead.Bulkhead;

/**
 * <p><b>舱壁（Bulkhead）</b>限制<strong>同时</strong>执行的资源调用数量（信号量或线程池语义），把不同依赖或不同业务隔离，
 * 避免某一个慢接口占满共享线程池，导致其他功能不可用。</p>
 *
 * <p><b>典型效果：</b>当并发中的执行数达到上限时，新调用可能被立即拒绝或等待（取决于 {@code maxWaitDuration} 等配置）；
 * 执行结束后释放容量。适合与线程池、异步链路配合，显式划定「这一块最多同时跑多少」。</p>
 *
 * <p><b>本类示范两种编程式用法：</b></p>
 * <ul>
 *   <li>{@link #demonstrateExecute}: 使用 {@link io.github.resilience4j.bulkhead.Bulkhead#executeSupplier} 在当前舱壁配额内执行。</li>
 *   <li>{@link #demonstrateDecorate}: 使用 {@link io.github.resilience4j.bulkhead.Bulkhead#decorateSupplier} 包装
 *       {@link Supplier}，便于把限并发逻辑与业务分离、多次触发。</li>
 * </ul>
 */
public final class BulkheadUsageExample {

    private static final Logger log = LoggerFactory.getLogger(BulkheadUsageExample.class);

    private BulkheadUsageExample() {
    }

    /** 在舱壁并发限制内执行一次示例调用。 */
    public static String demonstrateExecute(Bulkhead bulkhead) {
        return bulkhead.executeSupplier(BulkheadUsageExample::concurrentLimitedCall);
    }

    /** 返回受舱壁保护的 {@link Supplier}，在 {@code get()} 时申请/释放并发许可。 */
    public static String demonstrateDecorate(Bulkhead bulkhead) {
        Supplier<String> decorated =
                Bulkhead.decorateSupplier(bulkhead, BulkheadUsageExample::concurrentLimitedCall);
        try {
            return decorated.get();
        } catch (RuntimeException e) {
            log.warn("bulkhead decorate path failed, name={}", bulkhead.getName(), e);
            throw e;
        }
    }

    private static String concurrentLimitedCall() {
        return "ok";
    }
}
