package com.kato.pro.langchain.domain.tool;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * 工具 SPI。
 *
 * 实现规则：
 *   - 必须标注 @ToolDef(name=..., type=...)
 *   - 必须标注 @Component（或 @Service）
 *   - execute() 内禁止再调 dispatcher（避免循环）；参数解析由实现自行处理
 *
 * v1 参数约定：JsonNode 是 LLM 结构化 tool_call 的 args（key-value tree）。
 */
public interface Tool {

    /** 工具名（与 @Tool.name 一致；为防御性冗余，registry 比对时会校验） */
    String name();

    /** 执行工具。异常由实现自行 catch 并返回 ToolResult.fail()，不抛到 invoker。 */
    ToolResult execute(JsonNode args, ToolContext ctx);
}
