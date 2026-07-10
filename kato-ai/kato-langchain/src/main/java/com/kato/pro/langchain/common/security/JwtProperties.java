package com.kato.pro.langchain.common.security;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * JWT 配置（jwt.*）。
 *
 *   - secret      : HMAC-SHA256 密钥（≥ 32 字节）
 *   - ttl-seconds : token 有效期（默认 3600 = 1h）
 *   - issuer      : 签发者（默认 "kato-langchain"）
 *   - header      : HTTP header 名（默认 "Authorization"）
 *   - prefix      : token 前缀（默认 "Bearer "）
 */
@Data
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {
    private String secret = "kato-langchain-default-secret-key-must-be-at-least-32-bytes";
    private long ttlSeconds = 3600;
    private String issuer = "kato-langchain";
    private String header = "Authorization";
    private String prefix = "Bearer ";
}
