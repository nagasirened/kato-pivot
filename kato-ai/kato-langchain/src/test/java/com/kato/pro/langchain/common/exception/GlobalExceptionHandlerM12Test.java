package com.kato.pro.langchain.common.exception;

import com.kato.pro.langchain.common.result.Result;
import com.kato.pro.resilience.aspect.CircuitOpenException;
import com.kato.pro.resilience.aspect.RateLimitException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;

/**
 * M12 新增异常 → HTTP 状态码映射测试（spec §6 M12）。
 *
 *   - RateLimitException   → HTTP 429 + ErrorCode.RATE_LIMITED(1004)
 *   - CircuitOpenException → HTTP 503 + ErrorCode.CIRCUIT_OPEN(1005)
 */
class GlobalExceptionHandlerM12Test {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleRateLimit_returns429WithRateLimitedCode() {
        ResponseEntity<Result<Object>> resp = handler.handleRateLimit(
                new RateLimitException("rate limited: chat.send t:1"));
        assertEquals(HttpStatus.TOO_MANY_REQUESTS, resp.getStatusCode());
        assertNotNull(resp.getBody());
        assertEquals(1004, resp.getBody().getCode());
        assertEquals(ErrorCode.RATE_LIMITED.getMessage(), resp.getBody().getMessage());
    }

    @Test
    void handleCircuitOpen_returns503WithCircuitOpenCode() {
        ResponseEntity<Result<Object>> resp = handler.handleCircuitOpen(
                new CircuitOpenException("circuit open: sync.trigger"));
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, resp.getStatusCode());
        assertNotNull(resp.getBody());
        assertEquals(1005, resp.getBody().getCode());
        assertEquals(ErrorCode.CIRCUIT_OPEN.getMessage(), resp.getBody().getMessage());
    }

    @Test
    void handleRateLimit_preservesOriginalMessageInternally() {
        // 对外返回的是 ErrorCode.RATE_LIMITED 默认 message；
        // 但 verify 内部不会把原始的 rate-limited 详情直接泄漏（这条已通过 message == 默认值覆盖）
        ResponseEntity<Result<Object>> resp = handler.handleRateLimit(
                new RateLimitException("内部 trace: t:1 = 100/秒"));
        assertEquals("请求过于频繁，请稍后再试", resp.getBody().getMessage(),
                "对外不应回显原始 trace");
    }

    @Test
    void handleCircuitOpen_doesNotEchoCause() {
        // 类似：上游错误细节不应回显
        ResponseEntity<Result<Object>> resp = handler.handleCircuitOpen(
                new CircuitOpenException("sync trigger failed at 10.0.0.42: stack-xyz"));
        assertEquals("服务暂不可用，请稍后再试", resp.getBody().getMessage(),
                "对外不应回显上游 trace");
    }

    @Test
    void errorCode_rateLimitedExistsAndHasCorrectNumber() {
        // 锁住 enum 顺序，避免后人误改 code → 破坏 SLA 兼容性
        assertEquals(1004, ErrorCode.RATE_LIMITED.getCode());
        assertNotNull(ErrorCode.RATE_LIMITED.getMessage());
    }

    @Test
    void errorCode_circuitOpenExistsAndHasCorrectNumber() {
        assertEquals(1005, ErrorCode.CIRCUIT_OPEN.getCode());
        assertNotNull(ErrorCode.CIRCUIT_OPEN.getMessage());
    }

    @Test
    void errorCode_newCodesDoNotCollideWithExisting() {
        // 1004 / 1005 在 1xxx 段且不与既有业务码冲突
        assertNotEquals(ErrorCode.PARAM_INVALID.getCode(), ErrorCode.RATE_LIMITED.getCode());
        assertNotEquals(ErrorCode.RESOURCE_NOT_FOUND.getCode(), ErrorCode.RATE_LIMITED.getCode());
        assertNotEquals(ErrorCode.DUPLICATE_RESOURCE.getCode(), ErrorCode.RATE_LIMITED.getCode());
        assertNotEquals(ErrorCode.PARAM_INVALID.getCode(), ErrorCode.CIRCUIT_OPEN.getCode());
        assertNotEquals(ErrorCode.RESOURCE_NOT_FOUND.getCode(), ErrorCode.CIRCUIT_OPEN.getCode());
        assertNotEquals(ErrorCode.DUPLICATE_RESOURCE.getCode(), ErrorCode.CIRCUIT_OPEN.getCode());
    }
}
