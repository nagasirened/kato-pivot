package com.kato.pro.auth.util;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.StrUtil;
import com.kato.pro.base.entity.LoginUser;
import com.kato.pro.auth.constant.OAuth2Constant;
import com.kato.pro.auth.constant.TokenException;
import com.kato.pro.auth.thread.UserContextHolder;
import io.jsonwebtoken.Claims;
import lombok.experimental.UtilityClass;

import javax.servlet.http.HttpServletRequest;

@UtilityClass
public class SecurityUtil {

    public String getHeaderToken(HttpServletRequest request) {
        return request.getHeader(OAuth2Constant.HEADER_TOKEN);
    }

    /**
     * 从request获取jwt-token
     * @param request       HttpServletRequest
     * @return              String
     */
    public String getToken(HttpServletRequest request) {
        String headerToken = getHeaderToken(request);
        if (StrUtil.isBlank(headerToken)) {
            return null;
        }
        return TokenUtil.getToken(headerToken);
    }

    /**
     * 从token中获取Claims对象
     * @param token     密钥
     * @return          Claims
     */
    public Claims getClaims(String token) {
        if (StrUtil.isBlank(token)) {
            return null;
        }
        try {
            return TokenUtil.getClaims(token);
        } catch (Exception e) {
            throw new TokenException("token失效");
        }
    }

    /**
     * 从token中获取用户基本信息
     * @param request       HttpServletRequest
     * @return              LoginUser
     */
    public LoginUser getLoginUser(HttpServletRequest request) {
        String token = getToken(request);
        Claims claims = getClaims(token);
        LoginUser loginUser = LoginUser.builder().userId(Convert.toStr(claims.get(OAuth2Constant.USER_ID)))
                .account(Convert.toStr(claims.get(OAuth2Constant.USER_NAME)))
                .roleId(Convert.toStr(claims.get(OAuth2Constant.ROLE_ID)))
                .tenantId(Convert.toStr(claims.get(OAuth2Constant.TENANT_ID)))
                .type(Convert.toInt(claims.get(OAuth2Constant.TYPE)))
                .build();
        UserContextHolder.setUser(loginUser);
        return loginUser;
    }



}
