package com.kato.pro.resilience.aspect;

/**
 * 限流拒绝异常（spec §6 M12）。
 *
 * 抛出时由 GlobalExceptionHandler 翻译为 HTTP 429 或业务错误码 RATE_LIMITED。
 */
public class RateLimitException extends RuntimeException {
    public RateLimitException(String message) {
        super(message);
    }
}
