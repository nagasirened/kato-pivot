package com.kato.pro.resilience.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 限流注解（spec §6 M12）。
 *
 * 标注在 controller 方法上：调用前由 RateLimitAspect 拦截，
 * 通过 resilience4j RateLimiter 控制 QPS。
 *
 * 用例：
 *   @RateLimited(key = "chat.send", keyBy = KeyBy.TENANT, permitsPerSecond = 20)
 *   @RateLimited(key = "api.admin.tool.audit.approve", keyBy = KeyBy.USER, permitsPerSecond = 5)
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimited {
    /** 限流器逻辑名（实际 instanceName = key + "." + resolveKey(keyBy)） */
    String key();

    /** 限流 key 维度 */
    KeyBy keyBy() default KeyBy.GLOBAL;

    /** 每秒允许的请求数（仅 key 维度首次出现时生效；后续相同 key 沿用配置） */
    int permitsPerSecond() default 10;

    /** 获取许可的超时（0 = 立即拒绝；>0 = 等到拿到许可） */
    long timeoutMs() default 0;
}
