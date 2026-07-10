package com.kato.pro.langchain.api.prompt.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class RenderResponse {
    String key;
    String content;
    String source;
    Integer version;
}
