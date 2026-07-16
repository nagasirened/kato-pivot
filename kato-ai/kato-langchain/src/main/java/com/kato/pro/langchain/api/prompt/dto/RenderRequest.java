package com.kato.pro.langchain.api.prompt.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.Map;

@Data
@Schema(description = "Prompt 渲染请求体")
public class RenderRequest {

    @NotBlank(message = "key 不能为空")
    @Schema(description = "模板 key",
            example = "chat.system",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String key;

    @Schema(description = "模板变量（key → value 占位）",
            example = "{\"userName\": \"alice\", \"date\": \"2026-07-15\"}",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private Map<String, Object> vars;
}
