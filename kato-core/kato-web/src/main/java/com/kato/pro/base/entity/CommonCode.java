package com.kato.pro.base.entity;


import lombok.Getter;

public enum CommonCode {

    OK(200, "ok"),
    SYSTEM_ERROR(5000, "系统错误"),
    PARAM_ERROR(5001, "参数错误"),
    REQUEST_RATE_LIMIT(5002, "请求被限制"),
    ;

    @Getter
    private final Integer code;
    @Getter
    private final String message;

    CommonCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }

}
