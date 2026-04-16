package com.kato.pro.resilience4j.example.timelimiter;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.resilience4j.timelimiter.TimeLimiter;

/**
 * <p><b>超时（Time Limiter）</b>为异步 {@link java.util.concurrent.CompletionStage} 设置最大允许执行时间；
 * 超时可取消仍在进行中的任务（取决于配置），防止调用方无限等待慢或卡住的下游。</p>
 *
 * <p><b>典型效果：</b>在调度器上包装 {@code CompletionStage}，若在 {@code timeoutDuration} 内未正常完成，
 * 则失败并触发超时异常；成功完成则不受影响。常与异步 HTTP、并行 RPC、CompletableFuture 链配合。</p>
 *
 * <p><b>本类示范两种编程式用法：</b>均需传入 {@link ScheduledExecutorService}，用于限时调度与回调。</p>
 * <ul>
 *   <li>{@link #demonstrateExecute}: 使用 {@link TimeLimiter#executeCompletionStage} 直接得到带超时控制的
 *       {@link CompletionStage}。</li>
 *   <li>{@link #demonstrateDecorate}: 使用实例方法 {@link TimeLimiter#decorateCompletionStage} 得到
 *       {@code Supplier<CompletionStage<T>>}，再调用 {@code get()} 启动阶段链。</li>
 * </ul>
 */
public final class TimeLimiterUsageExample {

    private static final Logger log = LoggerFactory.getLogger(TimeLimiterUsageExample.class);

    private TimeLimiterUsageExample() {
    }

    /**
     * 在超时限制下执行异步阶段；内部通过 {@code supplyAsync} 模拟异步工作（示例中为立即完成）。
     */
    public static CompletionStage<String> demonstrateExecute(TimeLimiter timeLimiter,
            ScheduledExecutorService scheduler) {
        return timeLimiter.executeCompletionStage(scheduler,
                () -> CompletableFuture.supplyAsync(TimeLimiterUsageExample::slowAsyncCall, scheduler));
    }

    /**
     * 先构造「带超时」的阶段供应商，再 {@code get()} 触发执行，适合把供应商作为参数传递的场景。
     */
    public static CompletionStage<String> demonstrateDecorate(TimeLimiter timeLimiter,
            ScheduledExecutorService scheduler) {
        Supplier<CompletionStage<String>> decorated = timeLimiter.decorateCompletionStage(scheduler,
                () -> CompletableFuture.supplyAsync(TimeLimiterUsageExample::slowAsyncCall, scheduler));
        try {
            return decorated.get();
        } catch (RuntimeException e) {
            log.warn("timeLimiter decorate path failed, name={}", timeLimiter.getName(), e);
            throw e;
        }
    }

    private static String slowAsyncCall() {
        return "ok";
    }

    /**
     * 当应用尚未对外提供统一的调度器 Bean 时，可用此方法创建一个守护线程调度器（生产环境建议由 Spring 管理单例）。
     */
    public static ScheduledExecutorService defaultScheduler() {
        return Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "kato-timelimiter-example");
            t.setDaemon(true);
            return t;
        });
    }
}
