package com.kato.pro.langchain.common.result;

import com.kato.pro.langchain.common.exception.ErrorCode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ResultTest {

    @Test
    void ok_noData_returnsOkCode() {
        Result<String> r = Result.ok();
        assertEquals(200, r.getCode());
        assertNull(r.getData());
    }

    @Test
    void ok_withData_returnsOkCodeAndData() {
        Result<String> r = Result.ok("hello");
        assertEquals(200, r.getCode());
        assertEquals("hello", r.getData());
    }

    @Test
    void ok_withMessageAndData_returnsOkCodeCustomMessage() {
        Result<String> r = Result.ok("done", "value");
        assertEquals(200, r.getCode());
        assertEquals("done", r.getMessage());
        assertEquals("value", r.getData());
    }

    @Test
    void fail_withErrorCode_returnsErrorCodeAndDefaultMessage() {
        Result<String> r = Result.fail(ErrorCode.RESOURCE_NOT_FOUND);
        assertEquals(1002, r.getCode());
        assertEquals("资源不存在", r.getMessage());
        assertNull(r.getData());
    }

    @Test
    void fail_withCustomMessage_overridesMessage() {
        Result<String> r = Result.fail(ErrorCode.PARAM_INVALID, "tenant_id 不能为空");
        assertEquals(1001, r.getCode());
        assertEquals("tenant_id 不能为空", r.getMessage());
    }
}
