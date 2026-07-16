package com.kato.pro.resilience.aspect;

import com.kato.pro.resilience.annotation.RateLimited;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 限流 AOP 拦截器（spec §6 M12）。
 *
 * instanceName = "key.dimension-value"（如 "chat.send.t:1"）。
 * 首次出现新 key 时按注解参数生成 RateLimiterConfig 注册到 registry。
 */
@Slf4j
@Aspect
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 100) // 在 RoleAspect 之后（鉴权通过才限流）
public class RateLimitAspect {

    private final RateLimiterRegistry registry;
    private final KeyResolver keyResolver;

    public RateLimitAspect(RateLimiterRegistry registry, KeyResolver keyResolver) {
        this.registry = registry;
        this.keyResolver = keyResolver;
    }

    @Around("@annotation(rateLimited)")
    public Object around(ProceedingJoinPoint pjp, RateLimited rateLimited) throws Throwable {
        String dim = keyResolver.resolve(rateLimited.keyBy());
        String instanceName = rateLimited.key() + "." + dim;
        RateLimiter limiter = registry.rateLimiter(instanceName, customConfig(rateLimited));
        Duration timeout = Duration.ofMillis(rateLimited.timeoutMs());
        try {
            return RateLimiter.decorateCheckedSupplier(limiter, () -> {
                try {
                    return pjp.proceed();
                } catch (Throwable t) {
                    throw new RuntimeException(t);
                }
            }).get();
        } catch (Throwable t) {
            // 区分：限流拒绝 vs 业务异常
            if (t instanceof RateLimitException rle) throw rle;
            // resilience4j 内部 RequestNotPermitted 抛 RuntimeException
            String msg = t.getMessage() == null ? "" : t.getMessage();
            if (msg.contains("RateLimiter") || t.getClass().getSimpleName().contains("NotPermitted")) {
                log.warn("RateLimited: {} (instance={})", rateLimited.key(), instanceName);
                throw new RateLimitException("rate limited: " + rateLimited.key());
            }
            if (t instanceof RuntimeException re) throw re;
            throw new RuntimeException(t);
        }
    }

    private static RateLimiterConfig customConfig(RateLimited rateLimited) {
        return RateLimiterConfig.custom()
                .limitForPeriod(rateLimited.permitsPerSecond())
                .limitRefreshPeriod(Duration.ofSeconds(1))
                .timeoutDuration(Duration.ofMillis(rateLimited.timeoutMs()))
                .build();
    }
}
