package com.kato.pro.auth.util;

import cn.hutool.core.util.ObjectUtil;
import com.kato.pro.auth.constant.LoginUser;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class KatoAuthUser {

    /**
     * 获取Authentication
     */
    private Authentication getAuthentication() {
        return SecurityContextHolder.getContext().getAuthentication();
    }

    /**
     * 获取用户
     */
    public LoginUser getUser() {
        Authentication authentication = getAuthentication();
        return getUser(authentication);
    }

    public LoginUser getUser(Authentication authentication) {
        Object principal = authentication.getPrincipal();
        if (principal instanceof LoginUser) {
            return (LoginUser) principal;
        }
        return null;
    }

    /**
     * 获取用户名称
     */
    public String getUsername() {
        Authentication authentication = getAuthentication();
        return ObjectUtil.isNull(authentication) ? null : authentication.getName();
    }
}
