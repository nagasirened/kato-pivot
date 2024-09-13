package com.kato.pro.common.utils;

import com.kato.pro.common.exception.KatoServiceException;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ThrowUtil {

    public static void runtimeException(String msg) {
        throw new KatoServiceException(msg);
    }

}
