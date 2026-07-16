package com.kato.pro.resilience;

import com.kato.pro.resilience.aspect.CircuitBreakerAspect;
import com.kato.pro.resilience.aspect.KeyResolver;
import com.kato.pro.resilience.aspect.RateLimitAspect;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

/**
 * kato-resilience 自动装配入口（spec §6 M12）。
 *
 * 引入本 module 即获得：
 *   - @RateLimited 限流（resilience4j RateLimiter）
 *   - @CircuitBreaker 熔断（resilience4j CircuitBreaker）
 *   - KeyResolver 抽象（业务可覆盖）
 *
 * 注册：META-INF/spring.factories（兼容 Spring Boot 2.5/2.7/3.x） +
 *       META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports（3.x 优先）
 */
@Configuration
@EnableAspectJAutoProxy
public class KatoResilienceAutoConfiguration {

    @Bean
    public KeyResolver katoKeyResolver() {
        return new KeyResolver();
    }

    @Bean
    public RateLimitAspect katoRateLimitAspect(RateLimiterRegistry registry, KeyResolver keyResolver) {
        return new RateLimitAspect(registry, keyResolver);
    }

    @Bean
    public CircuitBreakerAspect katoCircuitBreakerAspect(CircuitBreakerRegistry registry) {
        return new CircuitBreakerAspect(registry);
    }
}
