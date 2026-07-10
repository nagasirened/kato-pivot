package com.kato.pro.langchain.api.chat.dto;

import lombok.Value;

@Value
public class SendMessageRequest {
    /** 消息内容（必填非空） */
    String content;
}
