package com.kato.pro.resilience4j.example.circuitBreaker;

import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;

/**
 * <p><b>断路器（Circuit Breaker）</b>用于在下游持续失败或明显变慢时，自动减少对该下游的调用，
 * 避免线程堆积、超时扩散和「雪崩」整条调用链。</p>
 *
 * <p><b>典型效果：</b>在统计窗口内失败率或慢调用比例超过阈值后，断路器会「打开」，
 * 短期内直接拒绝或快速失败（可配合降级）；经过等待时间后进入「半开」试探少量请求，
 * 成功则「关闭」恢复流量。打开期间可显著降低对不可用依赖的压力。</p>
 *
 * <p><b>本类示范两种编程式用法：</b></p>
 * <ul>
 *   <li>{@link #demonstrateExecute}: 直接通过封装对象的 {@code executeSupplier} 执行逻辑，由断路器统计结果。</li>
 *   <li>{@link #demonstrateDecorate}: 先用静态 {@code decorateSupplier} 得到装饰后的 {@link Supplier}，再按需多次 {@code get()}，
 *       适合把「已装饰的调用」当作可复用单元传递。</li>
 * </ul>
 */
public final class CircuitBreakerUsageExample {

    private static final Logger log = LoggerFactory.getLogger(CircuitBreakerUsageExample.class);

    private CircuitBreakerUsageExample() {
    }

    /**
     * 在当前断路器上下文中执行一次供应商逻辑；成功与异常均参与断路器状态统计。
     *
     * @param circuitBreaker 已由 Spring 注入或从 {@link io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry} 解析的实例
     * @param payload        业务入参示例
     * @return 供应商返回值
     */
    public static String demonstrateExecute(CircuitBreaker circuitBreaker, String payload) {
        return circuitBreaker.executeSupplier(() -> remoteCall(payload));
    }

    /**
     * 将供应商包装为受同一断路器保护的 {@link Supplier}，由调用方决定何时触发 {@code get()}。
     *
     * @param circuitBreaker 断路器实例
     * @return 装饰后执行的结果；若断路器已打开等，可能抛出运行时异常（如 {@code CallNotPermittedException}）
     */
    public static String demonstrateDecorate(CircuitBreaker circuitBreaker) {
        Supplier<String> decorated = CircuitBreaker.decorateSupplier(circuitBreaker, () -> remoteCall("decorated"));
        try {
            return decorated.get();
        } catch (RuntimeException e) {
            log.warn("circuitBreaker decorate path failed, name={}", circuitBreaker.getName(), e);
            throw e;
        }
    }

    private static String remoteCall(String payload) {
        return "ok:" + payload;
    }
}
