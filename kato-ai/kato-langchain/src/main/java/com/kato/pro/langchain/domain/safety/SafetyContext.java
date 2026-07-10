package com.kato.pro.langchain.domain.safety;

import lombok.Builder;
import lombok.Value;

/**
 * 检测上下文（direction 区分入参/出参；tenantId 用于多租户差异化策略）。
 */
@Value
@Builder
public class SafetyContext {
    public enum Direction { INPUT, OUTPUT }

    Direction direction;
    Long tenantId;
    /** filter 调用的 traceId（用于日志串联） */
    String traceId;
}
