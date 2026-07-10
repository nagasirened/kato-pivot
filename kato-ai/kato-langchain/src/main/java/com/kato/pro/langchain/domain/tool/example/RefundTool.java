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
 * 示例：申请退款（WRITE，A3 进审核队列）。
 *
 * 参数 schema：
 *   { "orderId": "string", "amount": number, "reason": "string" }
 *
 * v1：仅在审核通过后写一条 audit 结果，不接真实退款系统。
 */
@Slf4j
@Component
@ToolDef(name = "refund",
      type = ToolType.WRITE,
      description = "申请退款（写类，进审核队列）",
      schema = "{\"orderId\":\"string\",\"amount\":\"number\",\"reason\":\"string\"}")
public class RefundTool implements Tool {

    @Override
    public String name() { return "refund"; }

    @Override
    public ToolResult execute(JsonNode args, ToolContext ctx) {
        if (args == null || !args.has("orderId")) {
            return ToolResult.fail("orderId 不能为空");
        }
        String orderId = args.get("orderId").asText();
        double amount = args.has("amount") ? args.get("amount").asDouble() : 0.0;
        String reason = args.has("reason") ? args.get("reason").asText() : "(no reason)";
        log.info("RefundTool executed: orderId={}, amount={}, reason={}, approver={}",
                orderId, amount, reason, ctx.getUserId());

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("refundId", "RF" + System.currentTimeMillis());
        data.put("orderId", orderId);
        data.put("amount", amount);
        data.put("reason", reason);
        data.put("executedBy", ctx.getUserId());
        return ToolResult.ok(data);
    }
}
