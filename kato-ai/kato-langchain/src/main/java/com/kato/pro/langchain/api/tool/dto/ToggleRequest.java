package com.kato.pro.langchain.api.tool.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Value;

@Value
@Schema(description = "工具开关请求体")
public class ToggleRequest {

    @NotNull(message = "enabled 不能为空")
    @Schema(description = "true 启用，false 停用",
            example = "true",
            requiredMode = Schema.RequiredMode.REQUIRED)
    Boolean enabled;
}
