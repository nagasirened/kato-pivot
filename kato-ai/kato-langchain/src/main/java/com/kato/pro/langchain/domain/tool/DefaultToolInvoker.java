package com.kato.pro.langchain.domain.tool;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;

/**
 * ToolInvoker 默认实现。
 *
 *   - 共享 Executors.newCachedThreadPool（v1 简单；后续可换业务线程池）
 *   - CompletableFuture.supplyAsync + orTimeout 实现超时控制
 *   - SimpleCircuitBreaker 三态熔断
 *   - 异常一律转 ToolResult.fail()（dispatcher 不感知异常）
 */
@Slf4j
public class DefaultToolInvoker implements ToolInvoker {

    private static final AtomicLong POOL_ID = new AtomicLong();
    private final ExecutorService pool =
            Executors.newCachedThreadPool(r -> {
                Thread t = new Thread(r, "tool-invoker-" + POOL_ID.incrementAndGet());
                t.setDaemon(true);
                return t;
            });

    private final SimpleCircuitBreaker breaker;

    public DefaultToolInvoker(SimpleCircuitBreaker breaker) {
        this.breaker = breaker;
    }

    @Override
    public ToolResult invoke(Tool tool, JsonNode args, ToolContext ctx, long timeoutMs) {
        String name = tool.name();
        if (!breaker.allowRequest(name)) {
            log.warn("Circuit OPEN, reject tool={}", name);
            return ToolResult.fail("circuit open: " + name);
        }
        try {
            CompletableFuture<ToolResult> f = CompletableFuture.supplyAsync(
                    () -> tool.execute(args, ctx), pool);
            ToolResult r = f.get(timeoutMs, TimeUnit.MILLISECONDS);
            if (r != null && r.isSuccess()) {
                breaker.recordSuccess(name);
            } else {
                breaker.recordFailure(name);
            }
            return r == null ? ToolResult.fail("null result") : r;
        } catch (TimeoutException te) {
            breaker.recordFailure(name);
            log.warn("Tool timeout: name={} timeoutMs={}", name, timeoutMs);
            return ToolResult.fail("timeout after " + timeoutMs + "ms");
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            breaker.recordFailure(name);
            return ToolResult.fail("interrupted");
        } catch (ExecutionException ee) {
            breaker.recordFailure(name);
            log.error("Tool execute threw: name={}", name, ee.getCause());
            return ToolResult.fail("exception: " + (ee.getCause() == null ? "" : ee.getCause().getMessage()));
        } catch (Exception e) {
            breaker.recordFailure(name);
            log.error("Tool invoke unexpected error: name={}", name, e);
            return ToolResult.fail("exception: " + e.getMessage());
        }
    }
}
