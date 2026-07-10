package com.kato.pro.langchain.domain.tool;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 极简熔断器（v1，沙箱无 Redis 用 ThreadLocal/全局计数版）。
 *
 * 三态机：
 *   - CLOSED   : 正常；失败计数 < threshold → 继续放行
 *   - OPEN     : 失败计数 >= threshold；openedAt 记当前时间；resetMs 后尝试半开
 *   - HALF_OPEN: OPEN 超过 resetMs 后第一个请求试探；成功 → CLOSED；失败 → OPEN
 *
 * 线程安全：所有字段用 Atomic* 或 ConcurrentHashMap；同 toolName 全局共享计数器。
 */
public class SimpleCircuitBreaker {

    public enum State { CLOSED, OPEN, HALF_OPEN }

    public static class Counter {
        final AtomicInteger failures = new AtomicInteger();
        final AtomicLong openedAt = new AtomicLong(0);
        volatile State state = State.CLOSED;
    }

    private final ConcurrentHashMap<String, Counter> counters = new ConcurrentHashMap<>();
    private final int failureThreshold;
    private final long resetMs;

    public SimpleCircuitBreaker(int failureThreshold, long resetMs) {
        this.failureThreshold = failureThreshold;
        this.resetMs = resetMs;
    }

    /** 检查是否放行；返回 false 表示当前 OPEN/HALF_OPEN（拒绝） */
    public boolean allowRequest(String toolName) {
        Counter c = counters.computeIfAbsent(toolName, k -> new Counter());
        State s = c.state;
        if (s == State.CLOSED) return true;
        if (s == State.OPEN) {
            long elapsed = System.currentTimeMillis() - c.openedAt.get();
            if (elapsed >= resetMs) {
                c.state = State.HALF_OPEN;
                return true;
            }
            return false;
        }
        return false;
    }

    public void recordSuccess(String toolName) {
        Counter c = counters.computeIfAbsent(toolName, k -> new Counter());
        c.failures.set(0);
        c.state = State.CLOSED;
    }

    public void recordFailure(String toolName) {
        Counter c = counters.computeIfAbsent(toolName, k -> new Counter());
        int n = c.failures.incrementAndGet();
        if (n >= failureThreshold) {
            c.state = State.OPEN;
            c.openedAt.set(System.currentTimeMillis());
        }
    }

    public State stateOf(String toolName) {
        return counters.computeIfAbsent(toolName, k -> new Counter()).state;
    }
}
