package com.kato.pro.langchain.common.exception;

import lombok.Getter;

/**
 * 系统异常：表示系统级不可恢复错误（数据库连接失败、上游超时、配置缺失等）。
 * 应被日志记 error，通常伴随运维告警。给用户/调用方返回的 message 应隐藏敏感细节。
 */
@Getter
public class SystemException extends RuntimeException {

    private final ErrorCode errorCode;

    public SystemException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public SystemException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }
}
