package com.kato.pro.common.constant;


import lombok.Getter;

public enum CommonCode {

    OK(200, "ok"),
    SYSTEM_ERROR(5000, "系统错误"),
    PARAM_ERROR(5001, "参数错误"),
    REQUEST_RATE_LIMIT(5002, "请求被限制"),
    SECURITY_REQUIRE(5003, "未授权"),
    BEAN_NOT_EXIST(5004, "不存在的BEAN")
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
