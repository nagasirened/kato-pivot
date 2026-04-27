package com.kato.pro.server;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public final class LocalServiceCache {

    /**
     * key : 接口名称 + version
     * val : 具体的Bean对象
     */
    private static final Map<String, Object> beanMap = new ConcurrentHashMap<>();

    public static void store(String serviceVersion, Object bean) {
        beanMap.merge(serviceVersion, bean, (oldOne, newOne) -> {
            log.warn("service {} already registered, keeping existing one", serviceVersion);
            return oldOne;
        });
    }

    public static Object get(String serviceVersion) {
        return beanMap.get(serviceVersion);
    }

    public static List<String> getAllKeys() {
        return new ArrayList<>(beanMap.keySet());
    }
}
