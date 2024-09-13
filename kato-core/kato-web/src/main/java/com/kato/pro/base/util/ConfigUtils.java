package com.kato.pro.base.util;


import cn.hutool.core.lang.Assert;
import cn.hutool.core.text.CharSequenceUtil;
import com.kato.pro.common.constant.BaseConstant;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

/**
 * 1.获取配置文件中的内容
 * 2.可以用来获取SpringBean
 */
@Component
public class ConfigUtils implements ApplicationContextAware {

    private static ApplicationContext context;
    private static Environment environment;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        ConfigUtils.coverAttribute(applicationContext);
    }

    private static void coverAttribute(ApplicationContext applicationContext) {
        ConfigUtils.context = applicationContext;
        ConfigUtils.environment = applicationContext.getEnvironment();
    }

    /**
     * 获取Nacos配置信息, 该信息作为以后A/B实验用
     * @param key      配置的key，如 kato.redisson.enabled
     * @return              配置的值      true
     */
    public static String getProperty(String key) {
        return getProperty(key, "");
    }

    /**
     * 获取配置文件中的布尔类型属性值
     * @param key 属性键
     * @return 布尔类型属性值
     */
    public static String getProperty(String key, String defaultValue) {
        return Optional.ofNullable(environment.getProperty(key)).orElse(defaultValue);
    }

    /**
     * 获取配置文件中的布尔类型属性值
     * @param key 属性键
     * @return 布尔类型属性值
     */
    public static Boolean getBooleanProperty(String key) {
        return environment.getProperty(key, Boolean.class);
    }

    /**
     * 获取配置文件中的整型属性值
     * @param key 属性键
     * @return 整型属性值
     */
    public static Integer getIntegerProperty(String key) {
        return environment.getProperty(key, Integer.class);
    }

    /**
     * 如果abMap中包含配置，就使用abMap中的数据。
     * 否则从配置文件中获取，配置文件没有则直接使用默认值
     * @param abMap         abMap
     * @param key           属性键
     * @param defaultValue  兜底默认值
     * @return              字符串类型属性值
     */
    public static String getAbOrProperty(Map<String, String> abMap, String key, String defaultValue) {
        try {
            if (abMap.containsKey(key)) {
                return Optional.ofNullable(abMap.get(key)).orElse(defaultValue);
            }
            return Optional.ofNullable(environment.getProperty(key)).orElse(defaultValue);
        } catch (Exception ignored) {
            return defaultValue;
        }
    }

    /**
     * 快速检查某个配置是不是1，如果是1返回ture，否则返回false
     * @param abMap         abMap
     * @param key           属性键
     * @return              布尔类型属性值
     */
    public static Boolean checkRule(Map<String, String> abMap, String key) {
        return CharSequenceUtil.equals(BaseConstant.JUDGE_YES, getAbOrProperty(abMap, key, BaseConstant.JUDGE_NO));
    }

    /**
     * 从ApplicationContext中，根据beanName获取到bean的值
     * @param beanName          beanName
     * @param clazz             要获取的Bean的类型
     * @param <T>               泛型
     * @return                  Bean对象
     */
    public static <T> T getBean(String beanName, Class<T> clazz) {
        T bean = context.getBean(beanName, clazz);
        Assert.notNull(bean, "bean is not exists");
        return bean;
    }

}
