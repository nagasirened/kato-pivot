package com.kato.pro.uaa.handler;

import cn.hutool.core.lang.Assert;
import cn.hutool.core.text.CharSequenceUtil;
import com.kato.pro.base.entity.LoginAppUser;
import com.kato.pro.base.entity.SysRole;
import com.kato.pro.base.feign.UserClient;
import com.kato.pro.uaa.entity.base.AuthConstant;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;


public class KatoUserDetailService implements UserDetailsService {

    private final UserClient userClient;

    public KatoUserDetailService(UserClient userClient) {
        this.userClient = userClient;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Assert.notEmpty(username);
        // 用户尝试邮箱登录
        if (CharSequenceUtil.startWith(username, AuthConstant.MOBILE_PREFIX)) {
            String mobile = CharSequenceUtil.subAfter(username, AuthConstant.MOBILE_PREFIX, false);
            LoginAppUser authUser = userClient.findByMobile(mobile);
            return wrapUser(mobile, authUser);
        }

        LoginAppUser authUser = userClient.findByUsername(username);
        return wrapUser(username, authUser);
    }

    private UserDetails wrapUser(String username, LoginAppUser authUser) {
        if (authUser == null || authUser.getId() == null) {
            throw new UsernameNotFoundException("用户不存在");
        }
        return User.withUsername(username)
                .password(authUser.getPassword())
                .accountExpired(!authUser.isAccountNonExpired())
                .accountLocked(!authUser.isAccountNonLocked())
                .credentialsExpired(!authUser.isCredentialsNonExpired())
                .roles(authUser.getRoles().stream().map(SysRole::getCode).toArray(String[]::new))
                .authorities(authUser.getAuthorities())
                .build();
    }

}
