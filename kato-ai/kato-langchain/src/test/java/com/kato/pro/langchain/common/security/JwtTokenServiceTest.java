package com.kato.pro.langchain.common.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kato.pro.langchain.common.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * JwtTokenService 单元测试（手写 HS256，零依赖）。
 */
class JwtTokenServiceTest {

    private JwtTokenService service;
    private JwtProperties properties;

    @BeforeEach
    void setUp() {
        properties = new JwtProperties();
        properties.setSecret("kato-langchain-test-secret-32bytes-or-more-please");
        properties.setTtlSeconds(60);
        properties.setIssuer("test-issuer");
        properties.setPrefix("Bearer ");
        service = new JwtTokenService(properties, new ObjectMapper());
    }

    @Test
    void signAndParse_roundTrip() {
        AuthInfo info = AuthInfo.of(42L, "alice", 1L, Role.OPERATOR);
        String token = service.sign(info);
        assertNotNull(token);
        assertTrue(token.split("\\.").length == 3);

        AuthInfo parsed = service.parse(token);
        assertEquals(42L, parsed.userId());
        assertEquals("alice", parsed.username());
        assertEquals(1L, parsed.tenantId());
        assertEquals(Role.OPERATOR, parsed.role());
    }

    @Test
    void parse_expiredToken_throws() {
        AuthInfo info = AuthInfo.of(1L, "bob", 1L, Role.USER);
        String token = service.sign(info, -10L); // 已过期
        BusinessException ex = assertThrows(BusinessException.class, () -> service.parse(token));
        assertTrue(ex.getMessage().contains("过期"));
    }

    @Test
    void parse_tamperedSignature_throws() {
        AuthInfo info = AuthInfo.of(1L, "bob", 1L, Role.USER);
        String token = service.sign(info);
        // 篡改 signature
        String tampered = token.substring(0, token.length() - 4) + "AAAA";
        BusinessException ex = assertThrows(BusinessException.class, () -> service.parse(tampered));
        assertTrue(ex.getMessage().contains("签名"));
    }

    @Test
    void parse_malformedToken_throws() {
        BusinessException ex = assertThrows(BusinessException.class, () -> service.parse("not.a.real.jwt"));
        assertNotNull(ex);
    }

    @Test
    void extractFromHeader_stripsBearerPrefix() {
        assertEquals("abc.def.ghi", service.extractFromHeader("Bearer abc.def.ghi"));
        assertEquals("plain.token", service.extractFromHeader("plain.token"));
        assertEquals(null, service.extractFromHeader(null));
    }
}
