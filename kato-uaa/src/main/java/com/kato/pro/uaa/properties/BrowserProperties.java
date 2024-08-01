package com.kato.pro.uaa.properties;

import com.kato.pro.uaa.entity.base.LoginType;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BrowserProperties {

    /**
     * 如果用户配置了登录页面，则使用用户给的，没有的话用默认的这个uaa-login.html
     */
    private String loginPage = "/uaa-login.html";

    /**
     * 登录成功后返回类型，默认是JSON
     */
    private LoginType loginType = LoginType.JSON;

    /**
     * 默认开启rememberMe功能
     */
    private Boolean alwaysRememberMe = true;

    /**
     * rememberMe 超时时间，暂设置为3天
     */
    private Integer rememberMeTimes = 3600 * 24 * 3;

}
