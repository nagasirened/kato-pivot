package com.kato.pro.uaa.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kato.pro.uaa.entity.AuthConstant;
import com.kato.pro.uaa.entity.LoginType;
import com.kato.pro.uaa.properties.SecurityProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Slf4j
public class KatoAuthenticationLoginSuccessHandler extends SavedRequestAwareAuthenticationSuccessHandler {

    private final SecurityProperties securityProperties;
    private final ObjectMapper objectMapper;

    public KatoAuthenticationLoginSuccessHandler(SecurityProperties securityProperties, ObjectMapper objectMapper) {
        this.securityProperties = securityProperties;
        this.objectMapper = objectMapper;
    }

    /**
     * 登录成功后调用函数
     * @param authentication 登录成功的参数，里面封装了登录成功的一系列信息
     */
    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        log.info("login success, author is {}", authentication.getPrincipal());
        // 默认返回json，如果转发类型则跳转入参数路径
        if (LoginType.REDIRECT.equals(securityProperties.getBrowser().getLoginType())) {
            super.onAuthenticationSuccess(request, response, authentication);
        } else {
            response.setContentType(AuthConstant.DEFAULT_JSON_CONTENT_TYPE);
            response.getWriter().write(objectMapper.writeValueAsString(authentication));
        }
    }
}
