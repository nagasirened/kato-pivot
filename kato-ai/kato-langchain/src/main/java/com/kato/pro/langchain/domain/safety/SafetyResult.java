package com.kato.pro.langchain.domain.safety;

import lombok.Builder;
import lombok.Value;

import java.util.List;
import java.util.Map;

/**
 * 内容安全检测结果。
 *
 *   - passed         : 是否通过（false = 含违规）
 *   - hitWords       : 命中的关键词（用于日志）
 *   - sanitizedText  : 脱敏后的文本（filter 可选返回）
 *   - fallback       : 是否为降级结果（如 sensitive-client 不可用）
 *   - details        : 其它元数据（filter name、durationMs 等）
 */
@Value
@Builder
public class SafetyResult {
    boolean passed;
    List<String> hitWords;
    String sanitizedText;
    boolean fallback;
    Map<String, Object> details;

    public static SafetyResult pass() {
        return SafetyResult.builder().passed(true).hitWords(List.of()).fallback(false)
                .details(Map.of()).build();
    }

    public static SafetyResult passFallback(String reason) {
        return SafetyResult.builder().passed(true).hitWords(List.of()).fallback(true)
                .details(Map.of("reason", reason == null ? "" : reason)).build();
    }

    public static SafetyResult reject(List<String> hits, String sanitized) {
        return SafetyResult.builder().passed(false).hitWords(hits == null ? List.of() : hits)
                .sanitizedText(sanitized).fallback(false).details(Map.of()).build();
    }
}
