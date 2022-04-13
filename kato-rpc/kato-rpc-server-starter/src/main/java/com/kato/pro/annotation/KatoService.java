package com.kato.pro.annotation;

import org.springframework.stereotype.Service;

import java.lang.annotation.*;

/**
 * 带该注解的类，表示将要注册到服务注册中心里
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Service
public @interface KatoService {

    /**
     * 暴露的服务类型，这里应该填写实现的接口
     */
    Class<?> interfaceType() default Object.class;

    /**
     * 服务版本，默认为1.0
     */
    String version() default "1.0";

}