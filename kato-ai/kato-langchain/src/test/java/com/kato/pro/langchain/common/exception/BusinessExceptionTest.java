package com.kato.pro.langchain.common.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BusinessExceptionTest {

    @Test
    void constructor_withErrorCode_setsCodeAndMessage() {
        BusinessException ex = new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        assertEquals(ErrorCode.RESOURCE_NOT_FOUND, ex.getErrorCode());
        assertEquals("资源不存在", ex.getMessage());
    }

    @Test
    void constructor_withCustomMessage_overridesMessage() {
        BusinessException ex = new BusinessException(ErrorCode.PARAM_INVALID, "tenant_id 不能为空");
        assertEquals(ErrorCode.PARAM_INVALID, ex.getErrorCode());
        assertEquals("tenant_id 不能为空", ex.getMessage());
    }

    @Test
    void constructor_withCause_preservesCause() {
        Throwable cause = new IllegalStateException("inner");
        BusinessException ex = new BusinessException(ErrorCode.INTERNAL_ERROR, "wrapped", cause);
        assertSame(cause, ex.getCause());
    }
}
