package com.kato.pro.langchain.api.chat.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Value;

/**
 * 发送消息的请求体。
 */
@Value
@Schema(description = "发送消息请求体")
public class SendMessageRequest {

    @NotBlank(message = "content 不能为空")
    @Size(max = 4000, message = "content 长度不能超过 4000")
    @Schema(description = "用户消息文本",
            example = "你好，我想查询订单状态",
            requiredMode = Schema.RequiredMode.REQUIRED,
            minLength = 1,
            maxLength = 4000)
    String content;
}
