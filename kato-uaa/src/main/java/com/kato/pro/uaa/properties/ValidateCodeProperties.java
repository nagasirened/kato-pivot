package com.kato.pro.uaa.properties;

import lombok.Data;

/**
 * author: ZGF
 * context : PC端核心配置
 */

@Data
public class ValidateCodeProperties {

    /**
     * 验证码长度，默认为4
     */
    private Integer validateCodeLength = 4;

    /**
     * 超时时间，默认5分钟
     */
    private Integer expireTimeLong = 60 * 5;

    /**
     * 需要图片验证码拦截的请求，用逗号隔开
     */
    private String imageInterceptorUrls;

    /**
     * 需要sms验证码拦截的请求，用逗号隔开
     */
    private String smsInterceptorUrls;
}
