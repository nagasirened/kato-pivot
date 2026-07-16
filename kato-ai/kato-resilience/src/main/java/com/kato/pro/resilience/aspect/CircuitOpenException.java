package com.kato.pro.resilience.aspect;

/**
 * 熔断打开异常（spec §6 M12）。
 *
 * 抛出时由 GlobalExceptionHandler 翻译为 HTTP 503 或业务错误码 CIRCUIT_OPEN。
 */
public class CircuitOpenException extends RuntimeException {
    public CircuitOpenException(String message) {
        super(message);
    }
}
