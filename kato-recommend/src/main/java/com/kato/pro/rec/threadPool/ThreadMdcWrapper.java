package com.kato.pro.rec.threadPool;

import cn.hutool.core.util.IdUtil;
import org.slf4j.MDC;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import java.util.Map;
import java.util.concurrent.Callable;

public class ThreadMdcWrapper {

    public static <T> Callable<T> wrap(final Callable<T> callable, final Map<String, String> context, final RequestAttributes requestAttributes) {
        return () -> {
            RequestContextHolder.setRequestAttributes(requestAttributes);
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

    public static Runnable wrap(final Runnable runnable, final Map<String, String> context, final RequestAttributes requestAttributes) {
        return () -> {
            RequestContextHolder.setRequestAttributes(requestAttributes);
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
