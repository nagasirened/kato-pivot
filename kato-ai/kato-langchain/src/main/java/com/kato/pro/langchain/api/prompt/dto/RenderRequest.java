package com.kato.pro.langchain.api.prompt.dto;

import lombok.Data;

import java.util.Map;

@Data
public class RenderRequest {
    private String key;
    private Map<String, Object> vars;
}
