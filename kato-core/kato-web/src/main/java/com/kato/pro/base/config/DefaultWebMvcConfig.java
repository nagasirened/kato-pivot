package com.kato.pro.base.config;

import com.kato.pro.base.resolver.LoginUserArgumentResolver;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

public class DefaultWebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        // 注入用户信息
        resolvers.add(new LoginUserArgumentResolver());
    }
}
