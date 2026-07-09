package com.kato.pro.langchain.api.chat.dto;

import lombok.Data;

/**
 * 创建会话的请求体。
 * title 可空（系统会用首条消息自动生成）。
 */
@Data
public class CreateSessionRequest {
    private String title;
}
