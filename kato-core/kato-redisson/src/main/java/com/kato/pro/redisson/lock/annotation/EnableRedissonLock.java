package com.kato.pro.redisson.lock.annotation;

import com.kato.pro.redisson.config.RedissonConfiguration;
import com.kato.pro.redisson.lock.aspect.KatoLockAspect;
import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

/**
 * 开启Redisson注册支持
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@Import({RedissonConfiguration.class, KatoLockAspect.class})
public @interface EnableRedissonLock {
}
