package com.kato.pro.langchain.common.exception;

import lombok.Getter;

/**
 * 统一错误码枚举。
 * 规则：1xxx=业务错误（用户可见），2xxx=鉴权/权限，5xxx=系统错误（用户不感知细节），9xxx=上游/外部错误。
 */
@Getter
public enum ErrorCode {

    OK(200, "ok"),

    // 1xxx 业务错误
    PARAM_INVALID(1001, "参数无效"),
    RESOURCE_NOT_FOUND(1002, "资源不存在"),
    DUPLICATE_RESOURCE(1003, "资源已存在"),

    // 2xxx 鉴权/权限
    UNAUTHORIZED(2001, "未登录或登录已过期"),
    FORBIDDEN(2003, "无权限访问"),
    TENANT_MISMATCH(2004, "租户上下文缺失或不匹配"),

    // 5xxx 系统错误
    INTERNAL_ERROR(5000, "系统内部错误"),
    DB_ERROR(5001, "数据库错误"),
    UPSTREAM_TIMEOUT(5002, "上游调用超时"),
    UPSTREAM_ERROR(5003, "上游调用失败");

    private final Integer code;
    private final String message;

    ErrorCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }
}
