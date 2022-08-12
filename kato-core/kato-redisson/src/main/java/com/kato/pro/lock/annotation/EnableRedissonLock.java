package com.kato.pro.lock.annotation;

import com.kato.pro.config.RedissonConfiguration;
import com.kato.pro.lock.aspect.KatoLockAspect;
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
