package com.kato.pro.res;


import lombok.Getter;

public enum ErrorCode {

    OK(200, "ok"),
    SYSTEM_ERROR(5000, "系统错误"),
    PARAM_ERROR(5001, "参数错误"),
    ;

    @Getter
    private final Integer code;
    @Getter
    private final String message;

    ErrorCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }

}
