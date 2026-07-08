package com.kato.pro.langchain.common.exception;

import com.kato.pro.langchain.common.result.Result;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleBusiness_returns200WithErrorCode() {
        BusinessException ex = new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "订单不存在");
        ResponseEntity<Result<Object>> resp = handler.handleBusiness(ex);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertNotNull(resp.getBody());
        assertEquals(1002, resp.getBody().getCode());
        assertEquals("订单不存在", resp.getBody().getMessage());
    }

    @Test
    void handleSystem_returns500WithGenericMessage() {
        SystemException ex = new SystemException(ErrorCode.DB_ERROR, "Connection refused at 10.0.0.1:3306");
        ResponseEntity<Result<Object>> resp = handler.handleSystem(ex);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, resp.getStatusCode());
        assertNotNull(resp.getBody());
        assertEquals(5001, resp.getBody().getCode());
        // 关键断言：对外不暴露敏感信息（"Connection refused at..." 不能泄露给客户端）
        assertEquals("数据库错误", resp.getBody().getMessage());
    }

    @Test
    void handleUnknown_returns500WithInternalErrorCode() {
        ResponseEntity<Result<Object>> resp = handler.handleUnknown(new RuntimeException("boom"));
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, resp.getStatusCode());
        assertNotNull(resp.getBody());
        assertEquals(5000, resp.getBody().getCode());
        assertEquals("系统内部错误", resp.getBody().getMessage());
    }
}
