package com.kato.pro.langchain.domain.tool;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * 工具调用执行器 SPI（O3：超时/重试/熔断/fallback）。
 *
 * 契约：
 *   - 实现内部保证 timeoutMs 内必须返回（超时 → ToolResult.fail("timeout")）
 *   - 异常 → ToolResult.fail()（不抛到 dispatcher）
 *   - 失败时调用 SimpleCircuitBreaker.recordFailure；连续触发阈值后熔断
 *   - 熔断中 → 直接返回 ToolResult.fail("circuit open")
 *
 * v1 默认实现：DefaultToolInvoker（CompletableFuture.supplyAsync + orTimeout）
 * 后续可替换：Resilience4jInvoker（接 kato-resilience4j 模块）
 */
public interface ToolInvoker {

    /** 执行工具；返回结果一定非 null（失败也返回 fail 结果） */
    ToolResult invoke(Tool tool, JsonNode args, ToolContext ctx, long timeoutMs);
}
