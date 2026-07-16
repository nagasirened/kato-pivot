package com.kato.pro.langchain.api.prompt.dto;

import lombok.Builder;
import lombok.Value;
import io.swagger.v3.oas.annotations.media.Schema;


@Value
@Builder
@Schema(description = "Prompt 渲染响应")
public class RenderResponse {
    String key;
    String content;
    String source;
    Integer version;
}
