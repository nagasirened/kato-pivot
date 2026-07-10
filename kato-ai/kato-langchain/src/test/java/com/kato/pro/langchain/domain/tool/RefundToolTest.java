package com.kato.pro.langchain.domain.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kato.pro.langchain.domain.tool.example.RefundTool;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RefundToolTest {

    private final RefundTool tool = new RefundTool();
    private final ObjectMapper mapper = new ObjectMapper();
    private final ToolContext ctx = ToolContext.builder()
            .tenantId(1L).userId(100L).traceId("t").channel("user").build();

    @Test
    void success_returnsRefundData() throws Exception {
        JsonNode args = mapper.readTree("{\"orderId\":\"ORD1\",\"amount\":99.5,\"reason\":\"damaged\"}");
        ToolResult r = tool.execute(args, ctx);
        assertTrue(r.isSuccess());
        assertEquals("ORD1", r.getData().get("orderId"));
        assertEquals(99.5, r.getData().get("amount"));
    }

    @Test
    void missingOrderId_returnsFail() {
        JsonNode args = mapper.createObjectNode();
        ToolResult r = tool.execute(args, ctx);
        assertFalse(r.isSuccess());
    }

    @Test
    void name_returnsRefund() {
        assertEquals("refund", tool.name());
    }

    @Test
    void defaultReason_applied() throws Exception {
        JsonNode args = mapper.readTree("{\"orderId\":\"ORD1\"}");
        ToolResult r = tool.execute(args, ctx);
        assertTrue(r.isSuccess());
        assertEquals("(no reason)", r.getData().get("reason"));
    }
}
