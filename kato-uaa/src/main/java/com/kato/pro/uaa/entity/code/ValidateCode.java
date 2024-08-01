package com.kato.pro.uaa.entity.code;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * author: ZGF
 * context : 验证码
 */

@Data
public class ValidateCode implements Serializable {

    /**
     * 临时Key，作用是作为验证码Key的一部分进行校验，校验的时候需要带上该prefix内容
     */
    private String validPrefix;

    private String code;

    private LocalDateTime expire;

    public ValidateCode(String code, Integer expireTimeLong) {
        this(code, LocalDateTime.now().plusSeconds(expireTimeLong));
    }

    public ValidateCode(String code, LocalDateTime expire) {
        this(code, expire, null);
    }

    public ValidateCode(String code, LocalDateTime expire, String validPrefix) {
        this.code = code;
        this.expire = expire;
        this.validPrefix = validPrefix;
    }

    public boolean isExpire(){
        return LocalDateTime.now().isAfter(expire);
    }

}
