package com.kato.pro.uaa.handler.valid.processor;

import com.kato.pro.uaa.entity.code.ValidateCode;
import com.kato.pro.uaa.handler.valid.sender.SmsCodeSender;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.ServletWebRequest;

import javax.annotation.Resource;

@Component("smsCodeProcessor")
public class SmsCodeProcessor extends AbstractValidateCodeProcessor<ValidateCode> {

    @Resource
    private SmsCodeSender smsCodeSender;

    /**
     * 实际，发送短信的方法
     */
    @Override
    protected void send(ServletWebRequest webRequest, ValidateCode validateCode) {
        smsCodeSender.sendSms(webRequest.getParameter("mobile"), validateCode.getCode());
    }

}