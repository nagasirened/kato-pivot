package com.kato.pro.langchain.domain.tool.example;

import com.fasterxml.jackson.databind.JsonNode;
import com.kato.pro.langchain.annotation.ToolDef;
import com.kato.pro.langchain.domain.tool.Tool;
import com.kato.pro.langchain.domain.tool.ToolContext;
import com.kato.pro.langchain.domain.tool.ToolResult;
import com.kato.pro.langchain.domain.tool.ToolType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 示例：订单查询（READ，A3 透传 userId）。
 *
 * v1 不接真实订单系统，返回 mock 数据。
 * spec §7.3 + 模块表 M7 示例场景。
 *
 * 参数 schema：
 *   { "orderId": "string" }
 *
 * 模拟：orderId 以 "FAIL-" 开头 → 工具失败；"SLOW-" 开头 → 模拟慢调用（用于超时/熔断测试）。
 */
@Slf4j
@Component
@ToolDef(name = "order_query",
      type = ToolType.READ,
      description = "查询订单状态（读类，userId 透传）",
      schema = "{\"orderId\":\"string\"}")
public class OrderQueryTool implements Tool {

    @Override
    public String name() { return "order_query"; }

    @Override
    public ToolResult execute(JsonNode args, ToolContext ctx) {
        if (args == null || !args.has("orderId") || args.get("orderId").asText().isBlank()) {
            return ToolResult.fail("orderId 不能为空");
        }
        String orderId = args.get("orderId").asText();
        log.info("OrderQueryTool invoked: orderId={}, userId={}, tenantId={}",
                orderId, ctx.getUserId(), ctx.getTenantId());

        if (orderId.startsWith("FAIL-")) {
            return ToolResult.fail("订单不存在: " + orderId);
        }
        if (orderId.startsWith("SLOW-")) {
            try { Thread.sleep(3000); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("orderId", orderId);
        data.put("status", "SHIPPED");
        data.put("trackingNo", "TN" + System.currentTimeMillis());
        data.put("userId", ctx.getUserId());
        data.put("tenantId", ctx.getTenantId());
        return ToolResult.ok(data);
    }
}
