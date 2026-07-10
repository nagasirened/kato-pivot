package com.kato.pro.langchain.api.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Value;

@Value
public class LoginRequest {
    @NotBlank(message = "username 不能为空")
    String username;
    @NotBlank(message = "password 不能为空")
    String password;
}
