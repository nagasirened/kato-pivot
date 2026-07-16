package com.kato.pro.langchain.api.safety.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Builder;
import lombok.Value;

/**
 * 内容安全检测请求。
 *
 *   - direction : INPUT(默认) / OUTPUT — 决定调用 checkInput 还是 checkOutput
 *   - text      : 待检测文本（必填，非空）
 */
@Value
@Builder
@Schema(description = "内容安全检测请求体")
public class SafetyCheckRequest {

    @Pattern(regexp = "INPUT|OUTPUT", message = "direction 必须是 INPUT 或 OUTPUT")
    @Schema(description = "检测方向", example = "INPUT",
            allowableValues = {"INPUT", "OUTPUT"},
            requiredMode = Schema.RequiredMode.REQUIRED)
    String direction;

    @NotBlank(message = "text 不能为空")
    @Schema(description = "待检测文本",
            example = "我要投诉你们",
            requiredMode = Schema.RequiredMode.REQUIRED,
            minLength = 1, maxLength = 8000)
    String text;
}
