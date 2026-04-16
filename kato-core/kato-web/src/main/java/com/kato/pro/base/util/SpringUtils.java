package com.kato.pro.base.util;

import cn.hutool.core.text.CharSequenceUtil;

/**
 * 从 Spring {@link org.springframework.core.env.Environment} 读取配置的统一入口，
 * 底层委托 {@link ConfigUtils}（需在 Spring 容器启动并完成 {@code ApplicationContextAware} 之后使用）。
 */
public final class SpringUtils {

    private SpringUtils() {
    }

    public static String getProperty(String key, String defaultValue) {
        return ConfigUtils.getProperty(key, defaultValue);
    }

    /**
     * 读取整型配置；未配置、无法解析或非数字时返回 defaultValue。
     */
    public static int getIntProperty(String key, int defaultValue) {
        Integer typed = ConfigUtils.getIntegerProperty(key);
        if (typed != null) {
            return typed;
        }
        String raw = ConfigUtils.getProperty(key);
        if (CharSequenceUtil.isBlank(raw)) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
