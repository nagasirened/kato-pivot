package com.kato.pro.rec.utilities;


import cn.hutool.core.util.StrUtil;
import com.google.common.base.Preconditions;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Map;
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
     * 获取Nacos配置信息, 该信息作为以后A/B实验用
     * @param property      配置的key，如 kato.redisson.enabled
     * @return              配置的值      true
     */
    public String getProperty(String property) {
        return getProperty(property, "");
    }

    public String getProperty(String property, String defaultValue) {
        return Optional.ofNullable(environment.getProperty(property)).orElse(defaultValue);
    }

    public String getAbOrProperty(Map<String, String> abMap, String property, String defaultValue) {
        try {
            if (abMap.containsKey(property)) {
                return Optional.ofNullable(abMap.get(property)).orElse(defaultValue);
            }
            return Optional.ofNullable(environment.getProperty(property)).orElse(defaultValue);
        } catch (Exception ignored) {
            return defaultValue;
        }
    }

    public Boolean checkRule(Map<String, String> abMap, String property, String defaultValue) {
        return StrUtil.equals("1", getAbOrProperty(abMap, property, defaultValue));
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
