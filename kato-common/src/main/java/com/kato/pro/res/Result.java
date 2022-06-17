package com.kato.pro.res;

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

    public static <T> Result<T> build(ErrorCode errorCode) {
        return build(errorCode, null);
    }

    public static <T> Result<T> build(T t) {
        return build(ErrorCode.OK, t);
    }

    public static <T> Result<T> build(String message, T t) {
        return new Result<>(ErrorCode.OK.getCode(), message, t);
    }

    public static <T> Result<T> build(ErrorCode errorCode, T t) {
        return new Result<>(errorCode.getCode(), errorCode.getMessage(), t);
    }

    public static <T> Result<T> build(Integer code, String message) {
        return new Result<>(code, message, null);
    }


}
