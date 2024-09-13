package com.kato.pro.base.annotation;


import java.lang.annotation.*;

/**
 * 分级日志
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ScaleLog { }
