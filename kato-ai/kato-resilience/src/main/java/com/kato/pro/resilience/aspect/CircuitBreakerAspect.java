package com.kato.pro.resilience.aspect;

import com.kato.pro.resilience.annotation.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

/**
 * 熔断 AOP 拦截器（spec §6 M12）。
 *
 * 通过 resilience4j CircuitBreakerRegistry 获取（或按需创建）熔断器，
 * 包裹原方法调用。CallNotPermittedException → 翻译为 CircuitOpenException。
 */
@Slf4j
@Aspect
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 110)
public class CircuitBreakerAspect {

    private final CircuitBreakerRegistry registry;

    public CircuitBreakerAspect(CircuitBreakerRegistry registry) {
        this.registry = registry;
    }

    @Around("@annotation(cb)")
    public Object around(ProceedingJoinPoint pjp, com.kato.pro.resilience.annotation.CircuitBreaker cb) throws Throwable {
        io.github.resilience4j.circuitbreaker.CircuitBreaker breaker =
                registry.circuitBreaker(cb.name());
        try {
            return io.github.resilience4j.circuitbreaker.CircuitBreaker.decorateCallable(breaker, () -> {
                try {
                    return pjp.proceed();
                } catch (Throwable t) {
                    if (t instanceof RuntimeException re) throw re;
                    throw new RuntimeException(t);
                }
            }).call();
        } catch (CallNotPermittedException notPermitted) {
            log.warn("Circuit open: {} (path={})", cb.name(), pjp.getSignature().toShortString());
            return invokeFallback(pjp, cb, notPermitted);
        } catch (Exception e) {
            // 业务异常：先尝试 fallback，否则透传
            Object fb = invokeFallback(pjp, cb, e);
            if (fb == FALLBACK_SENTINEL) throw e;
            return fb;
        }
    }

    private static final Object FALLBACK_SENTINEL = new Object();

    private Object invokeFallback(ProceedingJoinPoint pjp, com.kato.pro.resilience.annotation.CircuitBreaker cb, Throwable cause) {
        if (cb.fallbackMethod() == null || cb.fallbackMethod().isEmpty()) {
            if (cause instanceof CallNotPermittedException) {
                throw new CircuitOpenException("circuit open: " + cb.name());
            }
            return FALLBACK_SENTINEL;
        }
        try {
            MethodSignature sig = (MethodSignature) pjp.getSignature();
            Class<?> target = sig.getDeclaringType();
            Method fallback = target.getMethod(cb.fallbackMethod(), sig.getParameterTypes());
            if (fallback == null) {
                throw new CircuitOpenException("circuit open: " + cb.name() + " (fallback not found)");
            }
            return fallback.invoke(pjp.getTarget(), pjp.getArgs());
        } catch (Exception e) {
            throw new CircuitOpenException("circuit open + fallback failed: " + e.getMessage());
        }
    }
}
