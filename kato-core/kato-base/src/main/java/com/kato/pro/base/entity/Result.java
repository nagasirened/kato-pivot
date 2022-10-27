package com.kato.pro.base.entity;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class Result<T> {

    private Integer code;
    private String message;
    private T t;

    private Result(Integer code, String message, T t) {
        this.code = code;
        this.message = message;
        this.t = t;
    }

    public static <T> Result<T> build(CommonCode commonCode) {
        return build(commonCode, null);
    }

    public static <T> Result<T> build(T t) {
        return build(CommonCode.OK, t);
    }

    public static <T> Result<T> build(String message, T t) {
        return new Result<>(CommonCode.OK.getCode(), message, t);
    }

    public static <T> Result<T> build(CommonCode commonCode, T t) {
        return new Result<>(commonCode.getCode(), commonCode.getMessage(), t);
    }

    public static <T> Result<T> build(Integer code, String message) {
        return new Result<>(code, message, null);
    }

}
