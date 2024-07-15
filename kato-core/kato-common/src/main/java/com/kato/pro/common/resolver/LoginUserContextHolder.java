package com.kato.pro.common.resolver;

import com.alibaba.ttl.TransmittableThreadLocal;
import com.kato.pro.common.entity.LoginUser;

public class LoginUserContextHolder {

    public static final ThreadLocal<LoginUser> CONTEXT = new TransmittableThreadLocal<>();

    public static void setLoginUser(LoginUser loginUser) {
        CONTEXT.set(loginUser);
    }

    public static LoginUser getLoginUser() {
        return CONTEXT.get();
    }

    public static void clear() {
        CONTEXT.remove();
    }

}

