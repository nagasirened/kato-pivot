package com.kato.pro.langchain.api.auth.dto;

import com.kato.pro.langchain.common.security.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@Schema(description = "登录响应体")
public class LoginResponse {

    @Schema(description = "JWT token", example = "eyJhbGciOiJIUzI1NiJ9...")
    String token;

    @Schema(description = "token 有效期（秒）", example = "86400")
    long expiresIn;

    @Schema(description = "用户 ID", example = "1001")
    Long userId;

    @Schema(description = "用户名", example = "alice")
    String username;

    @Schema(description = "租户 ID", example = "1")
    Long tenantId;

    @Schema(description = "角色", example = "USER")
    Role role;
}
