package com.kato.pro.langchain.api.chat.dto;

import com.kato.pro.langchain.api.auth.dto.LoginRequest;
import com.kato.pro.langchain.api.knowledge.dto.SearchRequest;
import com.kato.pro.langchain.api.prompt.dto.RenderRequest;
import com.kato.pro.langchain.api.safety.dto.SafetyCheckRequest;
import com.kato.pro.langchain.api.tool.dto.ApproveRequest;
import com.kato.pro.langchain.api.tool.dto.ToggleRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * M13 DTO 字段校验生效测试（spec §6 M13）。
 *
 * 用 jakarta.validation.Validator 直接校验注解，验证 T4 注入的 @NotBlank/@Size/@Pattern/@Min/@Max
 * 全部按预期工作（不依赖 Spring 容器，沙箱 byte-buddy 限制下也跑得动）。
 */
class M13ValidationTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setUp() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        if (factory != null) factory.close();
    }

    private static Set<String> violationFields(Set<? extends ConstraintViolation<?>> v) {
        return v.stream().map(cv -> cv.getPropertyPath().toString()).collect(Collectors.toSet());
    }

    @Test
    void sendMessageRequest_emptyContent_isRejected() {
        SendMessageRequest req = new SendMessageRequest("");
        Set<ConstraintViolation<SendMessageRequest>> v = validator.validate(req);
        assertFalse(v.isEmpty());
        assertTrue(violationFields(v).contains("content"));
    }

    @Test
    void sendMessageRequest_overlongContent_isRejected() {
        String s = "a".repeat(4001);
        SendMessageRequest req = new SendMessageRequest(s);
        Set<ConstraintViolation<SendMessageRequest>> v = validator.validate(req);
        assertFalse(v.isEmpty());
        assertTrue(violationFields(v).contains("content"));
    }

    @Test
    void loginRequest_emptyFields_rejected() {
        LoginRequest req = new LoginRequest("", "");
        Set<ConstraintViolation<LoginRequest>> v = validator.validate(req);
        assertEquals(4, v.size(), "username+password each fail @NotBlank+@Size -> 4 violations");
        assertTrue(violationFields(v).containsAll(java.util.List.of("username", "password")));
    }

    @Test
    void loginRequest_shortUsername_rejected() {
        LoginRequest req = new LoginRequest("ab", "P@ssw0rd");
        Set<ConstraintViolation<LoginRequest>> v = validator.validate(req);
        assertFalse(v.isEmpty());
        assertTrue(violationFields(v).contains("username"));
    }

    @Test
    void searchRequest_blankQuery_rejected() {
        SearchRequest req = new SearchRequest();
        req.setQuery("");
        Set<ConstraintViolation<SearchRequest>> v = validator.validate(req);
        assertFalse(v.isEmpty());
        assertTrue(violationFields(v).contains("query"));
    }

    @Test
    void renderRequest_blankKey_rejected() {
        RenderRequest req = new RenderRequest();
        req.setKey("");
        Set<ConstraintViolation<RenderRequest>> v = validator.validate(req);
        assertFalse(v.isEmpty());
        assertTrue(violationFields(v).contains("key"));
    }

    @Test
    void safetyCheckRequest_invalidDirection_rejected() {
        SafetyCheckRequest req = SafetyCheckRequest.builder().direction("BOGUS").text("hi").build();
        Set<ConstraintViolation<SafetyCheckRequest>> v = validator.validate(req);
        assertFalse(v.isEmpty());
        assertTrue(violationFields(v).contains("direction"));
    }

    @Test
    void toggleRequest_nullEnabled_rejected() {
        ToggleRequest req = new ToggleRequest(null);
        Set<ConstraintViolation<ToggleRequest>> v = validator.validate(req);
        assertFalse(v.isEmpty());
        assertTrue(violationFields(v).contains("enabled"));
    }

    @Test
    void approveRequest_overlongComment_rejected() {
        String s = "x".repeat(501);
        ApproveRequest req = new ApproveRequest(s);
        Set<ConstraintViolation<ApproveRequest>> v = validator.validate(req);
        assertFalse(v.isEmpty());
        assertTrue(violationFields(v).contains("comment"));
    }
}
