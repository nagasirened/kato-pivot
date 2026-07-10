package com.kato.pro.langchain.domain.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kato.pro.langchain.domain.tool.example.OrderQueryTool;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrderQueryToolTest {

    private final OrderQueryTool tool = new OrderQueryTool();
    private final ObjectMapper mapper = new ObjectMapper();
    private final ToolContext ctx = ToolContext.builder()
            .tenantId(1L).userId(100L).traceId("t").channel("user").build();

    @Test
    void success_returnsOrderData() throws Exception {
        JsonNode args = mapper.readTree("{\"orderId\":\"ORD123\"}");
        ToolResult r = tool.execute(args, ctx);
        assertTrue(r.isSuccess());
        assertEquals("ORD123", r.getData().get("orderId"));
        assertEquals(100L, r.getData().get("userId"));
    }

    @Test
    void missingOrderId_returnsFail() {
        JsonNode args = mapper.createObjectNode();
        ToolResult r = tool.execute(args, ctx);
        assertFalse(r.isSuccess());
        assertTrue(r.getError().contains("orderId"));
    }

    @Test
    void failPrefix_returnsFail() throws Exception {
        JsonNode args = mapper.readTree("{\"orderId\":\"FAIL-999\"}");
        ToolResult r = tool.execute(args, ctx);
        assertFalse(r.isSuccess());
        assertTrue(r.getError().contains("不存在"));
    }

    @Test
    void name_returnsOrderQuery() {
        assertEquals("order_query", tool.name());
    }
}
