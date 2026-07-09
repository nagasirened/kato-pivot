package com.kato.pro.langchain.domain.session;

import com.kato.pro.langchain.common.exception.BusinessException;
import com.kato.pro.langchain.common.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChatMessageServiceTest {

    private ChatMessageService service;

    @BeforeEach
    void setup() {
        service = new ChatMessageService(null);
    }

    // ===== validateContent =====

    @Test
    void validateContent_null_throws() {
        BusinessException ex = assertThrows(BusinessException.class, () -> service.validateContent(null));
        assertEquals(ErrorCode.PARAM_INVALID, ex.getErrorCode());
    }

    @Test
    void validateContent_empty_throws() {
        assertThrows(BusinessException.class, () -> service.validateContent(""));
    }

    @Test
    void validateContent_whitespace_throws() {
        assertThrows(BusinessException.class, () -> service.validateContent("   \n\t  "));
    }

    @Test
    void validateContent_normal_returnsStripped() {
        assertEquals("hello", service.validateContent("  hello  "));
    }

    @Test
    void validateContent_tooLong_throws() {
        String tooLong = "a".repeat(ChatMessageService.MAX_CONTENT_LENGTH + 1);
        BusinessException ex = assertThrows(BusinessException.class, () -> service.validateContent(tooLong));
        assertTrue(ex.getMessage().contains("超长"));
    }

    @Test
    void validateContent_atMaxLength_succeeds() {
        String atMax = "a".repeat(ChatMessageService.MAX_CONTENT_LENGTH);
        assertEquals(atMax, service.validateContent(atMax));
    }

    // ===== 边界：listSessionMessages 校验（无 mapper 也走参数校验） =====

    @Test
    void listSessionMessages_nullSessionId_throws() {
        assertThrows(BusinessException.class, () -> service.listSessionMessages(null, 1, 20));
    }

    @Test
    void loadRecentMessages_nullSessionId_throws() {
        assertThrows(BusinessException.class, () -> service.loadRecentMessages(null, 20));
    }
}
