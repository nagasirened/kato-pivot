package com.kato.pro.auth.util;

import cn.hutool.core.text.CharSequenceUtil;
import com.kato.pro.auth.constant.OAuth2Constant;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import lombok.experimental.UtilityClass;

import java.util.Base64;

@UtilityClass
public class TokenUtil {

    public static final String TOKEN_PREFIX = "bearer";

    /**
     * 获取token字符串
     * @return      String
     */
    public static String getToken(String headerToken) {
        if (CharSequenceUtil.isBlank(headerToken) || headerToken.length() < 7) {
            return null;
        }
        if (CharSequenceUtil.equalsIgnoreCase(headerToken.substring(0, 6), TOKEN_PREFIX)) {
            return headerToken.substring(7);
        }
        return null;
    }


    /**
     * 获取jwt中的claims信息
     *
     * @param token     密钥
     * @return claim
     */
    public static Claims getClaims(String token) {
        String key = Base64.getEncoder().encodeToString(OAuth2Constant.SIGN_KEY.getBytes());
        return Jwts.parser().setSigningKey(key).parseClaimsJws(token).getBody();
    }

}
