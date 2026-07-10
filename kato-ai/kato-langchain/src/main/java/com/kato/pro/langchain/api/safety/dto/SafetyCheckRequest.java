package com.kato.pro.langchain.api.safety.dto;

import lombok.Builder;
import lombok.Value;

/**
 * 内容安全检测请求。
 *
 *   - direction : INPUT(默认) / OUTPUT — 决定调用 checkInput 还是 checkOutput
 *   - text      : 待检测文本（必填，非空）
 */
@Value
@Builder
public class SafetyCheckRequest {
    String direction;
    String text;
}
