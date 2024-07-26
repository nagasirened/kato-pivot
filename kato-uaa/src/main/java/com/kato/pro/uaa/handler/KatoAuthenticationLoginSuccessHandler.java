package com.kato.pro.uaa.handler;

import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class KatoAuthenticationLoginSuccessHandler implements AuthenticationSuccessHandler {

    /**
     * 登录成功后调用函数
     * @param authentication 登录成功的参数，里面封装了登录成功的一系列信息
     */
    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {

    }
}
