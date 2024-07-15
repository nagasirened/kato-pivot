package com.kato.pro.common.resolver;

import com.alibaba.ttl.TransmittableThreadLocal;

public class DeviceContextHolder {

    public static final ThreadLocal<String> CONTEXT = new TransmittableThreadLocal<>();

    public static void setDeviceId(String loginUser) {
        CONTEXT.set(loginUser);
    }

    public static String getDeviceId() {
        return CONTEXT.get();
    }

    public static void clear() {
        CONTEXT.remove();
    }

}
