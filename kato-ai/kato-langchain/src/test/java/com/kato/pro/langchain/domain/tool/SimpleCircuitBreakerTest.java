package com.kato.pro.langchain.domain.tool;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SimpleCircuitBreakerTest {

    @Test
    void closed_initially_allowsAll() {
        SimpleCircuitBreaker cb = new SimpleCircuitBreaker(3, 1000);
        assertTrue(cb.allowRequest("t"));
        assertEquals(SimpleCircuitBreaker.State.CLOSED, cb.stateOf("t"));
    }

    @Test
    void open_afterThresholdFailures() {
        SimpleCircuitBreaker cb = new SimpleCircuitBreaker(3, 1000);
        cb.recordFailure("t");
        cb.recordFailure("t");
        cb.recordFailure("t");
        assertEquals(SimpleCircuitBreaker.State.OPEN, cb.stateOf("t"));
        assertFalse(cb.allowRequest("t"));
    }

    @Test
    void success_resetsFailureCount() {
        SimpleCircuitBreaker cb = new SimpleCircuitBreaker(3, 1000);
        cb.recordFailure("t");
        cb.recordFailure("t");
        cb.recordSuccess("t");
        cb.recordFailure("t");
        cb.recordFailure("t");
        assertEquals(SimpleCircuitBreaker.State.CLOSED, cb.stateOf("t"));
    }

    @Test
    void halfOpen_afterResetMs() throws InterruptedException {
        SimpleCircuitBreaker cb = new SimpleCircuitBreaker(2, 100);
        cb.recordFailure("t");
        cb.recordFailure("t");
        assertFalse(cb.allowRequest("t"));
        Thread.sleep(120);
        assertTrue(cb.allowRequest("t"));
    }

    @Test
    void recordSuccess_closesFromHalfOpen() {
        SimpleCircuitBreaker cb = new SimpleCircuitBreaker(2, 0);
        cb.recordFailure("t");
        cb.recordFailure("t");
        cb.allowRequest("t");
        cb.recordSuccess("t");
        assertEquals(SimpleCircuitBreaker.State.CLOSED, cb.stateOf("t"));
        assertTrue(cb.allowRequest("t"));
    }
}
