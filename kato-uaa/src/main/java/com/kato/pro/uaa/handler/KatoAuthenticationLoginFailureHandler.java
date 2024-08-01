package com.kato.pro.uaa.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kato.pro.uaa.entity.base.LoginType;
import com.kato.pro.uaa.properties.SecurityProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Slf4j
@Component
public class KatoAuthenticationLoginFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    private final SecurityProperties securityProperties;
    private final ObjectMapper objectMapper;

    public KatoAuthenticationLoginFailureHandler(SecurityProperties securityProperties, ObjectMapper objectMapper) {
        this.securityProperties = securityProperties;
        this.objectMapper = objectMapper;
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException e) throws IOException, ServletException {
        log.info("login failure, exception is {}", e.getMessage(), e);
        if (LoginType.REDIRECT.equals(securityProperties.getBrowser().getLoginType())) {
            super.onAuthenticationFailure(request, response, e);
        } else {
            response.setContentType("application/json;charset=UTF-8");
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.getWriter().write(objectMapper.writeValueAsString("登录失败"));
        }
    }
}
