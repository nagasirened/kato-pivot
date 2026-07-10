package com.kato.pro.langchain.domain.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.kato.pro.langchain.annotation.ToolDef;
import com.kato.pro.langchain.common.metrics.ToolMetrics;
import com.kato.pro.langchain.common.tenant.TenantContext;
import com.kato.pro.langchain.common.trace.TraceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 工具分发器（A3 + R2 + O3 + L2 编排核心）。
 *
 * 流程：
 *   1. enabled 检查（全局开关）
 *   2. registry.require(name)  → Tool 实例
 *   3. tenant 启用检查（R2 — TenantToolConfigService）
 *   4. 构造 ToolContext（tenantId/userId/traceId/channel）
 *   5. WRITE 工具 → auditService.enqueue(...) → ToolResult("已提交待审核")
 *      READ 工具  → invoker.invoke(...) → 真实结果
 *   6. 返回 ToolResult（永不为 null；失败也返回 fail 结构）
 *
 * 注意：M8 ChatEngine 调 dispatcher 时不需要 try/catch；失败已在 dispatcher 内部转 ToolResult.fail。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ToolDispatcher {

    private final ToolProperties properties;
    private final ToolRegistry registry;
    private final ToolInvoker invoker;
    private final TenantToolConfigService tenantConfigService;
    private final ToolAuditService auditService;

    public ToolResult dispatch(String toolName, JsonNode args) {
        if (!properties.isEnabled()) {
            return ToolResult.fail("tool framework disabled");
        }
        Tool tool = registry.require(toolName);
        ToolDef ann = tool.getClass().getAnnotation(ToolDef.class);
        // M11 metrics
        ToolMetrics.onDispatch(toolName, ann.type().name());
        try {
            return dispatchInternal(tool, ann, toolName, args);
        } catch (Exception e) {
            ToolMetrics.onFailure(toolName, e.getClass().getSimpleName());
            throw e;
        }
    }

    private ToolResult dispatchInternal(Tool tool, ToolDef ann, String toolName, JsonNode args) {
        Long tenantId = TenantContext.requireCurrent().tenantId();
        Long userId = TenantContext.requireCurrent().userId();

        // R2 — 租户启用检查
        if (!tenantConfigService.isEnabled(tenantId, toolName)) {
            ToolMetrics.onFailure(toolName, "tenant-disabled");
            return ToolResult.fail("tool disabled for tenant: " + toolName);
        }

        ToolContext ctx = ToolContext.builder()
                .tenantId(tenantId)
                .userId(userId)
                .traceId(TraceContext.current())
                .channel("user")
                .build();

        if (ann.type() == ToolType.WRITE) {
            // A3 — 写类进审核队列
            Long auditId = auditService.enqueue(toolName, ann.type().name(), args, ctx);
            return ToolResult.ok(Map.of(
                    "auditId", auditId,
                    "status", "PENDING",
                    "message", "写类工具已提交待审核"
            ));
        }

        // A3 — 读类透传 + O3 — Resilience4j 包装
        ToolResult r = invoker.invoke(tool, args, ctx, properties.getDefaultTimeoutMs());
        // 异步落审计（读类也写，便于追溯）
        auditService.recordReadInvocation(toolName, ann.type().name(), args, r, ctx);
        return r;
    }

    /** admin 审核通过后真正执行（WRITE 路径） */
    public ToolResult executeApproved(Long auditId) {
        return auditService.executeApproved(auditId);
    }
}
