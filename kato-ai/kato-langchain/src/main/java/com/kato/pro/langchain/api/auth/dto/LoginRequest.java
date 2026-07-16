package com.kato.pro.langchain.api.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Value;

@Value
@Schema(description = "登录请求体")
public class LoginRequest {

    @NotBlank(message = "username 不能为空")
    @Size(min = 3, max = 64, message = "username 长度必须在 3-64")
    @Schema(description = "用户名",
            example = "alice",
            requiredMode = Schema.RequiredMode.REQUIRED,
            minLength = 3,
            maxLength = 64)
    String username;

    @NotBlank(message = "password 不能为空")
    @Size(min = 6, max = 64, message = "password 长度必须在 6-64")
    @Schema(description = "密码",
            example = "P@ssw0rd",
            requiredMode = Schema.RequiredMode.REQUIRED,
            minLength = 6,
            maxLength = 64,
            format = "password")
    String password;
}
