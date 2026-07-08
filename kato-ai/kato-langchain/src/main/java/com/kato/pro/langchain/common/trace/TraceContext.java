package com.kato.pro.langchain.common.trace;

import org.slf4j.MDC;

import java.util.UUID;

/**
 * TraceId 上下文。
 *
 * 存储在 SLF4J MDC（key=traceId）中，所有日志框架（logback/log4j2）都能识别并输出。
 * 同时提供静态方法 current() / set() / clear() 便于业务代码显式读取或传递。
 */
public final class TraceContext {

    public static final String MDC_KEY = "traceId";

    private TraceContext() {
    }

    /** 获取当前 TraceId；未设置时返回 null。 */
    public static String current() {
        return MDC.get(MDC_KEY);
    }

    /** 设置 TraceId 到 MDC。 */
    public static void set(String traceId) {
        MDC.put(MDC_KEY, traceId);
    }

    /** 生成新的 TraceId 并放入 MDC，返回新值。 */
    public static String generate() {
        String id = UUID.randomUUID().toString().replace("-", "");
        set(id);
        return id;
    }

    /** 从 MDC 移除 TraceId。Filter 的 finally 必须调用。 */
    public static void clear() {
        MDC.remove(MDC_KEY);
    }
}
