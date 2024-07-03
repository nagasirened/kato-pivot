package com.kato.pro.auth.thread;


import com.kato.pro.common.entity.LoginUser;

public class UserContextHolder {

    private static ThreadLocal<LoginUser> userHolder = new ThreadLocal<>();

    public static LoginUser getUser() {
        return userHolder.get();
    }

    public static void setUser(LoginUser loginUser) {
        userHolder.set(loginUser);
    }

}
