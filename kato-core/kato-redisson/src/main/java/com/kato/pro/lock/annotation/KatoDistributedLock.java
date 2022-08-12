package com.kato.pro.lock.annotation;


import java.lang.annotation.*;

/**
 *
 * 分布式锁注解，可运用于函数或类上，
 *
 * tip: Inherited  如果一个类用上了@Inherited修饰的注解，那么其子类也会继承这个注解
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface KatoDistributedLock {

    /**
     * 分布式锁的名称
     */
    String name() default "kato-distributed-lock";

    /**
     * 前缀
     */
    String prefix() default "";

    /**
     * 后缀
     */
    String suffix() default "";

    /**
     * 过期时间，单位秒
     */
    int expire() default 10;

}
