package com.kato.pro.uaa.config;

import com.kato.pro.base.feign.UserClient;
import com.kato.pro.uaa.entity.AuthConstant;
import com.kato.pro.uaa.handler.KatoUserDetailService;
import com.kato.pro.uaa.properties.SecurityProperties;
import com.kato.pro.uaa.util.PwdEncoderUtil;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;

import javax.annotation.Resource;

@Configuration
public class BrowserSecurityConfiguration extends WebSecurityConfigurerAdapter {

    @Resource private SecurityProperties securityProperties;
    @Resource private UserClient userClient;

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.formLogin(
            login ->
                login.loginPage(AuthConstant.LOGIN_PAGE)
                .loginProcessingUrl(AuthConstant.LOGIN_FORM_ADDRESS)

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
        return PwdEncoderUtil.getDelegatingPasswordEncoder();
    }
}
