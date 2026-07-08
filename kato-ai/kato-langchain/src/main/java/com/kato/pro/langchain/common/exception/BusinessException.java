package com.kato.pro.langchain.common.exception;

import lombok.Getter;

/**
 * 业务异常：用于可预期的、应向用户/调用方返回明确错误信息的场景。
 * 与 SystemException 的区别：SystemException 表示系统级不可恢复错误，日志记 error；
 * BusinessException 表示业务校验/规则不通过，日志记 warn 即可。
 */
@Getter
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public BusinessException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }
}
