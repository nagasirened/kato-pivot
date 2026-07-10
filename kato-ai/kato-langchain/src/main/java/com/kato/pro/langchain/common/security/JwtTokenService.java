package com.kato.pro.langchain.common.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kato.pro.langchain.common.exception.BusinessException;
import com.kato.pro.langchain.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT 签发 / 解析（HS256，零外部依赖）。
 *
 * 格式：base64url(header).base64url(payload).base64url(hmac-sha256)
 *
 * header  : {"alg":"HS256","typ":"JWT"}
 * payload : {"sub":"<userId>","username":"...","tenantId":N,"role":"ADMIN",
 *            "iss":"kato-langchain","iat":N,"exp":N}
 *
 * v1：固定密钥从配置读；v2 引入 kid + 密钥轮换。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JwtTokenService {

    private final JwtProperties properties;
    private final ObjectMapper objectMapper;

    /** 签发 token（ttl 由 properties 决定） */
    public String sign(AuthInfo info) {
        return sign(info, properties.getTtlSeconds());
    }

    /** 签发 token（自定义 ttl 秒） */
    public String sign(AuthInfo info, long ttlSeconds) {
        if (info == null) throw new IllegalArgumentException("info must not be null");
        long now = System.currentTimeMillis() / 1000L;
        Map<String, Object> header = new HashMap<>();
        header.put("alg", "HS256");
        header.put("typ", "JWT");
        Map<String, Object> payload = new HashMap<>();
        payload.put("sub", info.userId().toString());
        payload.put("username", info.username());
        payload.put("tenantId", info.tenantId());
        payload.put("role", info.role().name());
        payload.put("iss", properties.getIssuer());
        payload.put("iat", now);
        payload.put("exp", now + ttlSeconds);

        try {
            String h = b64u(objectMapper.writeValueAsBytes(header));
            String p = b64u(objectMapper.writeValueAsBytes(payload));
            String signingInput = h + "." + p;
            String sig = b64u(hmacSha256(signingInput, properties.getSecret()));
            return signingInput + "." + sig;
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "JWT 签发失败: " + e.getMessage());
        }
    }

    /** 解析 token → AuthInfo；失败抛 UNAUTHORIZED */
    public AuthInfo parse(String token) {
        if (token == null || token.isBlank()) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "token 为空");
        }
        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "token 格式错误");
        }
        // 1. 验签
        String signingInput = parts[0] + "." + parts[1];
        String expectedSig;
        try {
            expectedSig = b64u(hmacSha256(signingInput, properties.getSecret()));
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "token 验签失败: " + e.getMessage());
        }
        if (!constantTimeEquals(expectedSig, parts[2])) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "token 签名不匹配");
        }
        // 2. 解析 payload
        Map<String, Object> payload;
        try {
            byte[] payloadBytes = Base64.getUrlDecoder().decode(parts[1]);
            payload = objectMapper.readValue(payloadBytes, Map.class);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "token payload 解析失败: " + e.getMessage());
        }
        // 3. 验过期
        long now = System.currentTimeMillis() / 1000L;
        Object exp = payload.get("exp");
        if (exp instanceof Number n) {
            if (now >= n.longValue()) {
                throw new BusinessException(ErrorCode.UNAUTHORIZED, "token 已过期");
            }
        }
        // 4. 构造 AuthInfo
        try {
            Long userId = Long.valueOf(payload.get("sub").toString());
            String username = payload.get("username").toString();
            Long tenantId = ((Number) payload.get("tenantId")).longValue();
            Role role = Role.fromString(payload.get("role").toString());
            return AuthInfo.of(userId, username, tenantId, role);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "token payload 字段缺失: " + e.getMessage());
        }
    }

    /** 从 HTTP header 提取 token（剥掉 prefix） */
    public String extractFromHeader(String headerValue) {
        if (headerValue == null) return null;
        String prefix = properties.getPrefix();
        if (prefix != null && !prefix.isEmpty() && headerValue.startsWith(prefix)) {
            return headerValue.substring(prefix.length()).trim();
        }
        return headerValue.trim();
    }

    // ---- base64url helpers ----

    private static String b64u(byte[] data) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(data);
    }

    private static byte[] hmacSha256(String input, String key) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return mac.doFinal(input.getBytes(StandardCharsets.UTF_8));
    }

    /** 常量时间字符串比较（防 timing attack） */
    private static boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) return false;
        return MessageDigest.isEqual(a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8));
    }
}
