package com.kato.pro.uaa.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kato.pro.base.feign.UserClient;
import com.kato.pro.uaa.entity.AuthConstant;
import com.kato.pro.uaa.handler.KatoAuthenticationLoginFailureHandler;
import com.kato.pro.uaa.handler.KatoAuthenticationLoginSuccessHandler;
import com.kato.pro.uaa.handler.KatoUserDetailService;
import com.kato.pro.uaa.properties.SecurityProperties;
import com.kato.pro.uaa.util.PwdEncoderUtil;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

import javax.annotation.Resource;

@Configuration
public class BrowserSecurityConfiguration extends WebSecurityConfigurerAdapter {

    @Resource private SecurityProperties securityProperties;
    @Resource private UserClient userClient;
    @Resource private ObjectMapper objectMapper;

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.formLogin(
            login ->
                login.loginPage(AuthConstant.LOGIN_PAGE)
                .loginProcessingUrl(AuthConstant.LOGIN_FORM_ADDRESS)
                .successHandler(authenticationSuccessHandler())
                .failureHandler(authenticationFailureHandler())
        );
        http.authorizeRequests((req) ->
            req.antMatchers(
                    securityProperties.getBrowser().getLoginPage()
                ).permitAll()
                .anyRequest()
                .authenticated()
        );
        http.csrf().disable();
    }

    @Bean
    public UserDetailsService userDetailsService() {
        return new KatoUserDetailService(userClient);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return PwdEncoderUtil.getDefaultDelegatingPasswordEncoder();
    }

    @Bean
    public AuthenticationSuccessHandler authenticationSuccessHandler() {
        return new KatoAuthenticationLoginSuccessHandler(securityProperties, objectMapper);
    }

    @Bean
    public AuthenticationFailureHandler authenticationFailureHandler() {
        return new KatoAuthenticationLoginFailureHandler(securityProperties, objectMapper);
    }
}
