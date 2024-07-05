package com.kato.pro.base.util;

import cn.hutool.core.convert.Convert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
@RefreshScope
public class ConfigUtil {

    private static Environment environment;

    @Autowired
    public void setEnvironment(Environment environment) {
        ConfigUtil.environment = environment;
    }

    /**
     * 获取配置文件中的属性值
     * @param key 属性键
     * @return 属性值
     */
    public static String getProperty(String key) {
        return environment.getProperty(key);
    }

    /**
     * 获取配置文件中的属性值，如果属性不存在则返回默认值
     * @param key 属性键
     * @param defaultValue 默认值
     * @return 属性值
     */
    public static String getProperty(String key, String defaultValue) {
        return environment.getProperty(key, defaultValue);
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
     * 获取配置文件中的整型属性值
     * @param key 属性键
     * @return 整型属性值
     */
    public static Integer getIntegerProperty(String key, String defaultValue) {
        return Convert.toInt(environment.getProperty(key, defaultValue));
    }

}
