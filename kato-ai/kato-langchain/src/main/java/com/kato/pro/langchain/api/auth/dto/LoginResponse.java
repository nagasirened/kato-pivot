package com.kato.pro.langchain.api.auth.dto;

import com.kato.pro.langchain.common.security.Role;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class LoginResponse {
    String token;
    long expiresIn;
    Long userId;
    String username;
    Long tenantId;
    Role role;
}
