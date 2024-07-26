package com.kato.pro.uaa.entity;

public class AuthConstant {
    private AuthConstant(){}

    // 登录页面接口，返回页面或者是返回json
    public static final String LOGIN_PAGE = "/authentication/require";
    // 验证登录路径，默认是/login，这相当于自定义
    public static final String LOGIN_FORM_ADDRESS = "/authentication/form";

    // 电话号码默认prefix
    public static final String MOBILE_PREFIX = "mobile-";
    public static final String DEFAULT_JSON_CONTENT_TYPE = "application/json;charset=UTF-8";

}
