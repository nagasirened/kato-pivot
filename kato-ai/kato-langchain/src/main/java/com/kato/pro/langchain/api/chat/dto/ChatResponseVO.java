package com.kato.pro.langchain.api.chat.dto;

import lombok.Builder;
import lombok.Value;
import io.swagger.v3.oas.annotations.media.Schema;


@Value
@Builder
@Schema(description = "对话响应体")
public class ChatResponseVO {
    Long sessionId;
    String reply;
    /** 触发的工具名（如果有）；可空 */
    String toolName;
    /** 工具执行结果的 auditId（写类工具时存在） */
    Long auditId;
}
