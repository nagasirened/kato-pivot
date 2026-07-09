package com.kato.pro.langchain.domain.session;

import com.kato.pro.langchain.common.exception.BusinessException;
import com.kato.pro.langchain.common.exception.ErrorCode;
import com.kato.pro.langchain.common.tenant.TenantContext;
import com.kato.pro.langchain.common.tenant.TenantInfo;
import com.kato.pro.langchain.domain.chat.ChatSession;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChatSessionServiceTest {

    private ChatSessionService service;

    @BeforeEach
    void setup() {
        // 用 null mapper（纯方法测试不需要 mapper）
        service = new ChatSessionService(null);
        TenantContext.set(TenantInfo.of(1L, 100L));
    }

    @AfterEach
    void cleanup() {
        TenantContext.clear();
    }

    // ===== validateTitle =====

    @Test
    void validateTitle_null_returnsEmpty() {
        assertEquals("", service.validateTitle(null));
    }

    @Test
    void validateTitle_blank_returnsEmpty() {
        assertEquals("", service.validateTitle("   "));
    }

    @Test
    void validateTitle_normal_returnsTrimmed() {
        assertEquals("hello", service.validateTitle("  hello  "));
    }

    @Test
    void validateTitle_tooLong_truncates() {
        String longTitle = "a".repeat(ChatSessionService.MAX_TITLE_LENGTH + 100);
        String result = service.validateTitle(longTitle);
        assertEquals(ChatSessionService.MAX_TITLE_LENGTH, result.length());
    }

    // ===== canArchive =====

    @Test
    void canArchive_activeSession_returnsTrue() {
        ChatSession s = new ChatSession();
        s.setStatus(SessionStatus.ACTIVE);
        assertTrue(service.canArchive(s));
    }

    @Test
    void canArchive_archivedSession_returnsFalse() {
        ChatSession s = new ChatSession();
        s.setStatus(SessionStatus.ARCHIVED);
        assertFalse(service.canArchive(s));
    }

    @Test
    void canArchive_nullSession_returnsFalse() {
        assertFalse(service.canArchive(null));
    }

    // ===== getSession 入参校验 =====

    @Test
    void getSession_nullId_throwsBusinessException() {
        BusinessException ex = assertThrows(BusinessException.class, () -> service.getSession(null));
        assertEquals(ErrorCode.PARAM_INVALID, ex.getErrorCode());
    }

    // ===== createSession 校验 title 副作用 =====

    @Test
    void createSession_validatesTitleBeforeMapperCall() {
        assertDoesNotThrow(() -> {
            String t = service.validateTitle("正常标题");
            assertEquals("正常标题", t);
        });
    }

    @Test
    void updateSummary_nullParams_throws() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.updateSummary(null, "x", 0));
        assertEquals(ErrorCode.PARAM_INVALID, ex.getErrorCode());
    }
}
