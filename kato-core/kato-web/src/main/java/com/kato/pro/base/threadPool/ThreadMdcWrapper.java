package com.kato.pro.base.threadPool;

import cn.hutool.core.util.IdUtil;
import org.slf4j.MDC;

import java.util.Map;
import java.util.concurrent.Callable;

public class ThreadMdcWrapper {

    public static <T> Callable<T> wrap(final Callable<T> callable, final Map<String, String> context) {
        return () -> {
            if (context == null) {
              MDC.clear();
            } else {
              MDC.setContextMap(context);
            }
            setTraceIdIfAbsent();
            try {
                return callable.call();
            } finally {
                MDC.clear();
            }
        };
    }

    public static Runnable wrap(final Runnable runnable, final Map<String, String> context) {
        return () -> {
            if (context == null) {
                MDC.clear();
            } else {
                MDC.setContextMap(context);
            }
            setTraceIdIfAbsent();
            try {
                runnable.run();
            } finally {
                MDC.clear();
            }
        };
    }

    private static void setTraceIdIfAbsent() {
        if (MDC.get("TraceId") == null) {
            MDC.put("TraceId", IdUtil.fastSimpleUUID());
        }
    }

}
