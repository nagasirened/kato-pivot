package com.kato.pro.annotation;


import javax.annotation.Resource;
import java.lang.annotation.*;

@Target({ElementType.CONSTRUCTOR, ElementType.METHOD, ElementType.PARAMETER, ElementType.FIELD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Resource
public @interface KatoResource {

    String version() default "1.0";

}
