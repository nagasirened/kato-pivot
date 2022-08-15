package com.kato.pro.utilities;


import com.google.common.base.Preconditions;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * 获取nacos中的配置
 */
@Component
public class NacosPropertyUtil implements ApplicationContextAware {

    private ApplicationContext context;

    private Environment environment;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.context = applicationContext;
        this.environment = applicationContext.getEnvironment();
    }

    /**
     * 获取Nacos配置信息
     * @param property      配置的key，如 kato.redisson.enabled
     * @return              配置的值      true
     */
    public String getProperty(String property) {
        return Optional.ofNullable(environment.getProperty(property)).orElse("");
    }

    /**
     * 从ApplicationContext中，根据beanName获取到bean的值
     * @param beanName          beanName
     * @param clazz             要获取的Bean的类型
     * @param <T>               泛型
     * @return                  Bean对象
     */
    public <T> T getBean(String beanName, Class<T> clazz) {
        T bean = context.getBean(beanName, clazz);
        Preconditions.checkNotNull(bean, "bean is not exists");
        return bean;
    }

}
