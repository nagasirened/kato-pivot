package com.kato.pro.langchain.domain.chat;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * 解析后的 tool_call（M8 LLM 回复中提取）。
 *
 *   - name : 工具名（与 @ToolDef.name 一致）
 *   - args : 参数（JsonNode 结构）
 *
 * 解析失败的 tool_call 不会被 ChatEngine 调用（safety 兜底）。
 */
public record ParsedToolCall(String name, JsonNode args) {}
