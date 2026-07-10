package com.kato.pro.langchain.domain.chat;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ToolCallParserTest {

    private final ToolCallParser parser = new ToolCallParser(new ObjectMapper());

    @Test
    void parsesSimpleJsonBlock() {
        String reply = "我需要查询订单。\n```json\n{\"name\":\"order_query\",\"args\":{\"orderId\":\"123\"}}\n```";
        Optional<ParsedToolCall> r = parser.parse(reply);
        assertTrue(r.isPresent());
        assertEquals("order_query", r.get().name());
        assertEquals("123", r.get().args().get("orderId").asText());
    }

    @Test
    void noJsonBlock_returnsEmpty() {
        Optional<ParsedToolCall> r = parser.parse("普通文本回复，没有 tool_call");
        assertFalse(r.isPresent());
    }

    @Test
    void invalidJson_returnsEmpty() {
        Optional<ParsedToolCall> r = parser.parse("```json\n{not json}\n```");
        assertFalse(r.isPresent());
    }

    @Test
    void missingName_returnsEmpty() {
        Optional<ParsedToolCall> r = parser.parse("```json\n{\"args\":{}}\n```");
        assertFalse(r.isPresent());
    }

    @Test
    void nullOrBlank_returnsEmpty() {
        assertFalse(parser.parse(null).isPresent());
        assertFalse(parser.parse("").isPresent());
        assertFalse(parser.parse("   ").isPresent());
    }

    @Test
    void missingArgs_defaultsToEmptyObject() throws Exception {
        String reply = "```json\n{\"name\":\"order_query\"}\n```";
        Optional<ParsedToolCall> r = parser.parse(reply);
        assertTrue(r.isPresent());
        assertEquals("order_query", r.get().name());
        assertEquals(0, r.get().args().size());
    }
}
