package com.kato.pro.uaa.handler.valid.generator;

import com.kato.pro.uaa.entity.code.ValidateCode;
import com.kato.pro.uaa.properties.SecurityProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.ServletWebRequest;

import javax.annotation.Resource;

/**
 * author: ZGF
 * context : 生成短信验证码
 */
@Component(value = "smsCodeGenerator")
public class SmsCodeGenerator implements ValidateCodeGenerator {

    @Resource
    private SecurityProperties securityProperties;

    /**
     * 随机生成验证码，可控制验证码长度
     */
    @Override
    public ValidateCode generateCode(ServletWebRequest request) {
        String randomNumeric = doGenerator(securityProperties.getValid().getValidateCodeLength());
        return new ValidateCode(randomNumeric, securityProperties.getValid().getExpireTimeLong());
    }
}
