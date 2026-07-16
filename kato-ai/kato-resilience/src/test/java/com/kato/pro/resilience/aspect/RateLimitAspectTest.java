package com.kato.pro.resilience.aspect;

import com.kato.pro.resilience.annotation.KeyBy;
import com.kato.pro.resilience.annotation.RateLimited;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * RateLimitAspect 集成测试（spec §6 M12）。
 *
 * 直接调用 aspect 验证：首次通过、超限拒绝、业务异常透传、共用 limiter。
 * JoinPoint 用 JDK 动态代理实现，避免引入 Mockito（与 Java 21 + m2 缓存的 mockito-core 不兼容）。
 */
class RateLimitAspectTest {

    private RateLimiterRegistry registry;
    private KeyResolver keyResolver;
    private RateLimitAspect aspect;

    @BeforeEach
    void setUp() {
        registry = RateLimiterRegistry.ofDefaults();
        keyResolver = new KeyResolver();
        aspect = new RateLimitAspect(registry, keyResolver);
    }

    @Test
    void proceed_underLimit_returnsResult() throws Throwable {
        RateLimited ann = buildAnnotation("test.allow", KeyBy.GLOBAL, 10, 0);
        AtomicInteger calls = new AtomicInteger();
        ProceedingJoinPoint pjp = stubJoinPoint(() -> { calls.incrementAndGet(); return "ok"; });
        Object result = aspect.around(pjp, ann);
        assertEquals("ok", result);
        assertEquals(1, calls.get());
    }

    @Test
    void proceed_overLimit_throwsRateLimitException() throws Throwable {
        RateLimited ann = buildAnnotation("test.block", KeyBy.GLOBAL, 1, 0);
        // 预热 1 次
        aspect.around(stubJoinPoint(() -> "first"), ann);

        // 第 2 次同 instance → 拒绝
        Throwable thrown = null;
        try {
            aspect.around(stubJoinPoint(() -> "second"), ann);
        } catch (Throwable t) {
            thrown = t;
        }
        assertNotNull(thrown, "second call should be rejected");
        assertTrue(thrown instanceof RateLimitException
                        || (thrown.getMessage() != null
                            && thrown.getMessage().toLowerCase().contains("rate")),
                "expected RateLimitException or rate-limit message, got: "
                        + thrown.getClass().getSimpleName() + ": " + thrown.getMessage());
    }

    @Test
    void proceed_businessException_propagates() throws Throwable {
        RateLimited ann = buildAnnotation("test.biz", KeyBy.GLOBAL, 10, 0);
        IllegalArgumentException biz = new IllegalArgumentException("bad arg");

        Throwable thrown = null;
        try {
            // supplier 抛 RuntimeException(biz)，aspect 应透传
            aspect.around(stubJoinPoint(() -> { throw new RuntimeException(biz); }), ann);
        } catch (Throwable t) {
            thrown = t;
        }
        assertNotNull(thrown);
        // 业务异常不应被翻译为 RateLimitException
        assertFalse(thrown instanceof RateLimitException,
                "business exception should not be wrapped as RateLimitException, got: "
                        + thrown.getClass().getSimpleName());
    }

    @Test
    void proceed_globalKey_isSharedAcrossInstances() throws Throwable {
        RateLimited ann = buildAnnotation("shared", KeyBy.GLOBAL, 50, 0);
        assertEquals("a", aspect.around(stubJoinPoint(() -> "a"), ann));
        assertEquals("b", aspect.around(stubJoinPoint(() -> "b"), ann));
        assertEquals(1, registry.getAllRateLimiters().size(),
                "shared GLOBAL key should reuse same limiter, got: "
                        + registry.getAllRateLimiters().stream()
                                .map(io.github.resilience4j.ratelimiter.RateLimiter::getName)
                                .toList());
    }

    @Test
    void annotation_fields_areReadCorrectly() {
        RateLimited ann = buildAnnotation("test.config", KeyBy.GLOBAL, 5, 0);
        assertEquals(5, ann.permitsPerSecond());
        assertEquals(0L, ann.timeoutMs());
        assertEquals(KeyBy.GLOBAL, ann.keyBy());
        assertEquals("test.config", ann.key());
    }

    // ---- helpers ----

    /**
     * 构造一个 ProceedingJoinPoint 代理，proceed() 调用 {@code fn}。
     * fn 可以抛 RuntimeException（aspect 内部会用 throw-wrapping 处理）。
     */
    private static ProceedingJoinPoint stubJoinPoint(ProcFn fn) {
        InvocationHandler handler = (proxy, method, args) -> {
            switch (method.getName()) {
                case "proceed":
                    return fn.run();
                case "toString":
                    return "stub-pjp";
                case "toShortString":
                case "toLongString":
                    return "stub";
                case "getStaticPart":
                case "getThis":
                case "getTarget":
                    return null;
                case "getArgs":
                    return new Object[0];
                case "getSignature":
                    return stubSignature();
                case "getKind":
                    return "method-execution";
                case "getSourceLocation":
                    return null;
                default:
                    return null;
            }
        };
        return (ProceedingJoinPoint) Proxy.newProxyInstance(
                ProceedingJoinPoint.class.getClassLoader(),
                new Class<?>[]{ProceedingJoinPoint.class},
                handler);
    }

    private static Signature stubSignature() {
        InvocationHandler sigHandler = (proxy, method, args) -> {
            switch (method.getName()) {
                case "toString":
                case "toShortString":
                case "toLongString":
                case "getName":
                case "getDeclaringTypeName":
                    return "stub";
                case "getModifiers":
                    return 0;
                case "getDeclaringType":
                    return Object.class;
                case "hashCode":
                    return 0;
                case "equals":
                    return proxy == args[0];
                default:
                    return null;
            }
        };
        return (Signature) Proxy.newProxyInstance(
                Signature.class.getClassLoader(),
                new Class<?>[]{Signature.class},
                sigHandler);
    }

    /** supplier-like 接口，可抛 RuntimeException。 */
    @FunctionalInterface
    interface ProcFn {
        Object run();
    }

    private static RateLimited buildAnnotation(String key, KeyBy by, int permits, long timeout) {
        return new RateLimited() {
            @Override public String key() { return key; }
            @Override public KeyBy keyBy() { return by; }
            @Override public int permitsPerSecond() { return permits; }
            @Override public long timeoutMs() { return timeout; }
            @Override public Class<? extends Annotation> annotationType() { return RateLimited.class; }
        };
    }
}
