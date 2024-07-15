package com.kato.pro.base.resolver;

import com.kato.pro.auth.util.SecurityUtil;
import com.kato.pro.common.constant.BaseConstant;
import com.kato.pro.common.entity.LoginUser;
import com.kato.pro.common.resolver.DeviceContextHolder;
import com.kato.pro.common.resolver.LoginUserContextHolder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@ConditionalOnClass(Filter.class)
public class UserDeviceFilter extends OncePerRequestFilter {

    /**
     * 将用户信息放进线程属性中，可以随时取用
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        try {
            LoginUser loginUser = SecurityUtil.getLoginUser(request);
            LoginUserContextHolder.setLoginUser(loginUser);

            String deviceId = request.getHeader(BaseConstant.DEVICE_ID);
            DeviceContextHolder.setDeviceId(deviceId);

            filterChain.doFilter(request, response);
        } finally {
            LoginUserContextHolder.clear();
            DeviceContextHolder.clear();
        }
    }
}
