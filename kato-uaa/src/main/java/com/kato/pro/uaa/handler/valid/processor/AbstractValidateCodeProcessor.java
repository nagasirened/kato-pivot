package com.kato.pro.uaa.handler.valid.processor;

import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.util.IdUtil;
import com.kato.pro.uaa.entity.code.ImageCode;
import com.kato.pro.uaa.entity.code.ValidateCode;
import com.kato.pro.uaa.handler.valid.generator.ValidateCodeGenerator;
import com.kato.pro.uaa.handler.valid.saver.ICodeManager;
import org.springframework.web.context.request.ServletWebRequest;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.Map;

public abstract class AbstractValidateCodeProcessor<T> implements ValidateCodeProcessor {

    /**
     * 通过接口，知道是处理sms还是image验证码请求：比如说/validate/sms，截取前缀后是sms,则是处理短信
     */
    public static final String API_PREFIX = "/validate/";

    @Resource
    private Map<String, ValidateCodeGenerator> validateCodeGeneratorMap;

    @Resource
    private ICodeManager iCodeManager;

    /**
     * 获取验证类型
     */
    private String getType(ServletWebRequest webRequest) {
        return CharSequenceUtil.subAfter(webRequest.getRequest().getRequestURI(), API_PREFIX, true);
    }

    @Override
    public void createValidateCode(ServletWebRequest webRequest) throws IOException {
        T validateCode = generateCode(webRequest);
        save(webRequest, validateCode);
        send(webRequest, validateCode);
    }

    @SuppressWarnings("all")
    private T generateCode(ServletWebRequest webRequest) {
        String type = getType(webRequest);
        ValidateCodeGenerator codeGenerator = validateCodeGeneratorMap.get(type + "CodeGenerator");
        return (T) codeGenerator.generateCode(webRequest);
    }

    protected void save(ServletWebRequest webRequest, T t) {
        ImageCode imageCode = (ImageCode) t;
        ValidateCode validateCode = new ValidateCode(imageCode.getCode(), imageCode.getExpire(), IdUtil.fastSimpleUUID());
        iCodeManager.save(webRequest, validateCode);
    }

    abstract void send(ServletWebRequest webRequest, T validateCode) throws IOException;

}
