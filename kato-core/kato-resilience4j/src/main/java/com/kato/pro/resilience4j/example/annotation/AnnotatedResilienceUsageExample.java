package com.kato.pro.resilience4j.example.annotation;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import com.kato.pro.resilience4j.KatoResilienceInstanceNames;

import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;

/**
 * Declarative (AOP) usage aligned with {@link KatoResilienceInstanceNames} YAML instances.
 * <p>
 * Requires {@code spring-boot-starter-aop} (or equivalent) on the application classpath so
 * aspects apply. Activate this bean with profile {@code kato-resilience4j-example}, or copy
 * patterns into your own {@code @Service} under a scanned package.
 */
@Service
@Profile("kato-resilience4j-example")
public class AnnotatedResilienceUsageExample {

    @CircuitBreaker(name = KatoResilienceInstanceNames.CircuitBreaker.INSTANCE_DEFAULT,
            fallbackMethod = "circuitDemoFallback")
    public String circuitDemo(String input) {
        return "circuit:" + input;
    }

    @SuppressWarnings("unused")
    private String circuitDemoFallback(String input, Throwable t) {
        return "circuit-fallback:" + input;
    }

    @Retry(name = KatoResilienceInstanceNames.Retry.INSTANCE_DEFAULT, fallbackMethod = "retryDemoFallback")
    public String retryDemo() {
        return "retry:ok";
    }

    @SuppressWarnings("unused")
    private String retryDemoFallback(Throwable t) {
        return "retry:fallback";
    }

    @RateLimiter(name = KatoResilienceInstanceNames.RateLimiter.INSTANCE_DEFAULT,
            fallbackMethod = "rateLimiterDemoFallback")
    public String rateLimiterDemo() {
        return "ratelimit:ok";
    }

    @SuppressWarnings("unused")
    private String rateLimiterDemoFallback(Throwable t) {
        return "ratelimit:fallback";
    }

    @Bulkhead(name = KatoResilienceInstanceNames.Bulkhead.INSTANCE_DEFAULT, fallbackMethod = "bulkheadDemoFallback")
    public String bulkheadDemo() {
        return "bulkhead:ok";
    }

    @SuppressWarnings("unused")
    private String bulkheadDemoFallback(Throwable t) {
        return "bulkhead:fallback";
    }

    @TimeLimiter(name = KatoResilienceInstanceNames.TimeLimiter.INSTANCE_DEFAULT,
            fallbackMethod = "timeLimiterDemoFallback")
    public CompletionStage<String> timeLimiterDemo() {
        return CompletableFuture.completedFuture("timelimit:ok");
    }

    @SuppressWarnings("unused")
    private CompletionStage<String> timeLimiterDemoFallback(Throwable t) {
        return CompletableFuture.completedFuture("timelimit:fallback");
    }
}
