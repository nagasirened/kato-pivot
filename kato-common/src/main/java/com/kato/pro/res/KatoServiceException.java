package com.kato.pro.res;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class KatoServiceException extends RuntimeException {

    private Integer code;

    private String message;

    public KatoServiceException(ErrorCode errorCode) {
        this.code = errorCode.getCode();
        this.message = errorCode.getMessage();
    }

    public KatoServiceException(Integer code, String message) {
        this.code = code;
        this.message = message;
    }

    public KatoServiceException(String message) {
        this.code = ErrorCode.SYSTEM_ERROR.getCode();
        this.message = message;
    }

}
