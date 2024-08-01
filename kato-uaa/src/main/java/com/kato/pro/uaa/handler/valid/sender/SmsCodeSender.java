package com.kato.pro.uaa.handler.valid.sender;

/**
 * author: ZGF
 * context : 阿里云，短信发送组件
 */

public interface SmsCodeSender {

    void sendSms(String mobile, String validateCode);
}
