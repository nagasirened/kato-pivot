package com.kato.pro.base.resolver;

import com.alibaba.ttl.TransmittableThreadLocal;
import com.kato.pro.base.entity.LoginUser;

public class LoginUserContextHolder {

    public static final ThreadLocal<LoginUser> CONTEXT = new TransmittableThreadLocal<>();

    public static void setLoginUser(LoginUser loginUser) {
        CONTEXT.set(loginUser);
    }

    public LoginUser getLoginUser() {
        return CONTEXT.get();
    }

    public static void clear() {
        CONTEXT.remove();
    }

}

