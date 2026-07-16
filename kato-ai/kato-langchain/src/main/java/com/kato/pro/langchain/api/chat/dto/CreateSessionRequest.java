package com.kato.pro.langchain.api.chat.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 创建会话的请求体。
 * title 可空（系统会用首条消息自动生成）。
 */
@Data
@Schema(description = "创建会话请求体")
public class CreateSessionRequest {

    @Schema(description = "会话标题（可空，将用首条消息自动生成）",
            example = "咨询订单 #12345",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED,
            maxLength = 100)
    private String title;
}
