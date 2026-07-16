package com.kato.pro.resilience.aspect;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * RateLimitException / CircuitOpenException 单元测试（spec §6 M12）。
 */
class RateLimitExceptionTest {

    @Test
    void rateLimitException_messageIsPreserved() {
        RateLimitException ex = new RateLimitException("chat.send t:1 已达上限");
        assertEquals("chat.send t:1 已达上限", ex.getMessage());
    }

    @Test
    void rateLimitException_isRuntimeException() {
        assertTrue(new RateLimitException("x") instanceof RuntimeException);
    }

    @Test
    void circuitOpenException_messageIsPreserved() {
        CircuitOpenException ex = new CircuitOpenException("sync.trigger 已熔断");
        assertEquals("sync.trigger 已熔断", ex.getMessage());
    }

    @Test
    void circuitOpenException_isRuntimeException() {
        assertTrue(new CircuitOpenException("x") instanceof RuntimeException);
    }
}
