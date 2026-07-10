package com.kato.pro.langchain.domain.chat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * ToolCall 解析器（v1）。
 *
 * 期望格式：LLM 回复中包含 ```json ... ``` 代码块（首个），块内为：
 *   { "name": "order_query", "args": { "orderId": "..." } }
 *
 * v2 切 LangChain4j ToolSpecification 时此文件作废。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ToolCallParser {

    /** 匹配 ```json ... ``` 块（DOTALL 让 . 跨行） */
    private static final Pattern JSON_BLOCK = Pattern.compile("```json\\s*(\\{.*?\\})\\s*```",
            Pattern.DOTALL);

    private final ObjectMapper objectMapper;

    /** 返回首个解析成功的 tool_call；空 Optional 表示没有 tool_call */
    public Optional<ParsedToolCall> parse(String llmReply) {
        if (llmReply == null || llmReply.isBlank()) return Optional.empty();
        Matcher m = JSON_BLOCK.matcher(llmReply);
        while (m.find()) {
            String json = m.group(1);
            try {
                JsonNode node = objectMapper.readTree(json);
                JsonNode nameNode = node.get("name");
                JsonNode argsNode = node.get("args");
                if (nameNode == null || nameNode.asText().isBlank()) continue;
                String name = nameNode.asText();
                JsonNode args = (argsNode == null || argsNode.isNull())
                        ? objectMapper.createObjectNode()
                        : argsNode;
                return Optional.of(new ParsedToolCall(name, args));
            } catch (Exception e) {
                log.warn("Failed to parse tool_call json block: {}", e.getMessage());
            }
        }
        return Optional.empty();
    }
}
