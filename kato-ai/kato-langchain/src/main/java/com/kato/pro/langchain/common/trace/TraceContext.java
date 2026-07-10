package com.kato.pro.langchain.common.trace;

import org.slf4j.MDC;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * TraceId 上下文。
 *
 * 存储在 SLF4J MDC（key=traceId）中，所有日志框架（logback/log4j2）都能识别并输出。
 * 同时提供静态方法 current() / set() / clear() 便于业务代码显式读取或传递。
 *
 * v2：用 TransmittableThreadLocal 替代 MDC（线程池透传）。
 * 当前实现：MDC 仅在线程本地有效，@Async / Scheduled 需手动 wrap。
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

    /**
     * 包裹一段代码，临时设置 traceId；执行完自动清理。
     * 用途：定时任务 / MQ consumer / 任何非 web 入口。
     */
    public static <T> T wrap(String traceId, Supplier<T> action) {
        String prev = current();
        set(traceId);
        try {
            return action.get();
        } finally {
            if (prev == null) {
                clear();
            } else {
                set(prev);
            }
        }
    }

    /** wrap(Runnable) 重载 */
    public static void runWith(String traceId, Runnable action) {
        wrap(traceId, () -> {
            action.run();
            return null;
        });
    }
}
