package com.kato.pro.uaa.handler.valid.generator;


import com.kato.pro.uaa.entity.code.ValidateCode;
import org.springframework.web.context.request.ServletWebRequest;

import java.util.Random;

/**
 * 生成验证码
 */
public interface ValidateCodeGenerator {

    ValidateCode generateCode(ServletWebRequest request);

    default String doGenerator(Integer length) {
        Random random = new Random();
        StringBuilder sRand = new StringBuilder();
        for(int i = 0; i < length; i++){
            String rand = String.valueOf(random.nextInt(10));
            sRand.append(rand);

        }
        return sRand.toString();
    }

}
