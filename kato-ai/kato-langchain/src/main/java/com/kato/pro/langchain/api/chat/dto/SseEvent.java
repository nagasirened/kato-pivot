package com.kato.pro.langchain.api.chat.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * SSE 流事件类型。
 *
 *   - START  : 流开始（含 sessionId）
 *   - CHUNK  : 文本分片
 *   - TOOL   : 工具调用结果
 *   - END    : 流结束（最终内容）
 *   - ERROR  : 流异常
 */
@Schema(description = "SSE 流事件（START/CHUNK/TOOL/END/ERROR）")
public record SseEvent(String type, String data, Long auditId, String toolName) {

    public static SseEvent start(Long sessionId) {
        return new SseEvent("START", String.valueOf(sessionId), null, null);
    }
    public static SseEvent chunk(String data) {
        return new SseEvent("CHUNK", data, null, null);
    }
    public static SseEvent tool(String toolName, Long auditId, String data) {
        return new SseEvent("TOOL", data, auditId, toolName);
    }
    public static SseEvent end(String finalReply) {
        return new SseEvent("END", finalReply, null, null);
    }
    public static SseEvent error(String msg) {
        return new SseEvent("ERROR", msg, null, null);
    }
}
