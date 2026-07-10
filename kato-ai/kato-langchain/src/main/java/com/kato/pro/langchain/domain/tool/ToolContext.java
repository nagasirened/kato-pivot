package com.kato.pro.langchain.domain.tool;

import lombok.Builder;
import lombok.Value;

/**
 * 工具调用上下文（不可变快照）。
 *
 *   - tenantId : 多租户隔离（R2）
 *   - userId   : 用户身份（A3 透传）
 *   - traceId  : 全链路串联（L2）
 *   - channel  : 来源渠道（admin / user / system），审计用
 */
@Value
@Builder
public class ToolContext {
    Long tenantId;
    Long userId;
    String traceId;
    String channel;
}
