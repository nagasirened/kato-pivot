package com.kato.pro.auth.annotation;

import java.lang.annotation.*;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface PreAuth {

    /**
     * 是否启用
     */
    boolean enable() default true;

    /**
     * 验证用户是否包含权限
     */
    String hasPerm() default "";

}
