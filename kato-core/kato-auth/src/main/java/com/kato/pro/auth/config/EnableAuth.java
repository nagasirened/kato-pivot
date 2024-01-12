package com.kato.pro.auth.config;

import com.kato.pro.auth.aspect.PreAuthAspect;
import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

/**
 * 开启Redisson注册支持
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@Import({TokenConfiguration.class})
public @interface EnableAuth {
}
