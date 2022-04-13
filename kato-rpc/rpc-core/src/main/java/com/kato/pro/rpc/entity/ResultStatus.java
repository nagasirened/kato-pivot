package com.kato.pro.rpc.entity;

import lombok.Getter;

public enum ResultStatus {

    SUCCESS((byte)1),
    FAIL((byte)0),
    ;

    @Getter
    private final byte code;

    ResultStatus(byte code) {
        this.code = code;
    }

    public static boolean isSuccess(byte code) {
        return ResultStatus.SUCCESS.code == code;
    }

}
