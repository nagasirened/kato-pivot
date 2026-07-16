package com.kato.pro.resilience.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 熔断器注解（spec §6 M12）。
 *
 * 标注在方法上：调用通过 resilience4j CircuitBreaker 包装。
 * 失败率超阈值 → 熔断打开 → 后续调用直接抛 CircuitOpenException。
 *
 * 用例：
 *   @CircuitBreaker(name = "upstream.minimax")
 *   @CircuitBreaker(name = "tool.dispatcher", fallbackMethod = "fallback")
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface CircuitBreaker {
    /** 熔断器名（resilience4j instance name，从 application.yml 配置） */
    String name();

    /** 可选：降级方法（同 class，参数列表相同，返回类型兼容） */
    String fallbackMethod() default "";
}
