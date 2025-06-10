package com.kato.pro.database;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

@Component
@Order(1) // 确保在事务之前执行
public class DataSourceAspect {
    
    @Pointcut("@annotation(com.yourpackage.annotation.DataSource)")
    public void dataSourcePointCut() {}
    
    @Before("dataSourcePointCut()")
    public void before(JoinPoint point) {
        MethodSignature signature = (MethodSignature) point.getSignature();
        Method method = signature.getMethod();
        
        DataSource dataSource = method.getAnnotation(DataSource.class);
        if (dataSource != null) {
            DynamicDataSource.setDataSourceType(dataSource.value());
        }
    }
    
    @After("dataSourcePointCut()")
    public void after(JoinPoint point) {
        DynamicDataSource.clearDataSourceType();
    }
}