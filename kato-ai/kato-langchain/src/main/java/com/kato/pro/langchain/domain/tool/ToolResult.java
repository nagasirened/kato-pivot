package com.kato.pro.langchain.domain.tool;

import lombok.Builder;
import lombok.Value;

import java.util.Map;

/**
 * 工具执行结果。
 *
 *   - success : 是否成功（false → 触发 fallback 文案）
 *   - data    : 结构化数据（admin 看 / 后续 prompt 拼装）
 *   - error   : 失败原因（成功时为 null）
 *   - meta    : 其它元数据（执行耗时、是否 fallback 等）
 */
@Value
@Builder
public class ToolResult {
    boolean success;
    Map<String, Object> data;
    String error;
    Map<String, Object> meta;

    public static ToolResult ok(Map<String, Object> data) {
        return ToolResult.builder().success(true).data(data)
                .meta(Map.of()).build();
    }

    public static ToolResult fail(String error) {
        return ToolResult.builder().success(false).error(error == null ? "" : error)
                .meta(Map.of()).build();
    }
}
