package com.kato.pro.common.utils;

import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.text.StrPool;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.servlet.http.HttpServletRequest;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * @author zlt
 * @date 2019/9/8
 */
@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class AddrUtil {
    private static final String UNKNOWN_STR = "unknown";
    /**
     * 获取客户端IP地址
     */
    public static String getRemoteAddr(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (isEmptyIP(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
            if (!isEmptyIP(ip)) return ip;
            ip = request.getHeader("WL-Proxy-Client-IP");
            if (!isEmptyIP(ip)) return ip;
            ip = request.getHeader("HTTP_CLIENT_IP");
            if (!isEmptyIP(ip)) return ip;
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
            if (!isEmptyIP(ip)) return ip;
            ip = request.getRemoteAddr();
            if ("127.0.0.1".equals(ip) || "0:0:0:0:0:0:0:1".equals(ip)) return getLocalAddr();    // 根据网卡取本机配置的IP
        } else if (ip.length() > 15) {
            String[] ips = ip.split(StrPool.COMMA);
            for (String strIp : ips) {
                if (!isEmptyIP(ip)) {
                    return strIp;
                }
            }
        }
        return ip;
    }

    private static boolean isEmptyIP(String ip) {
        return CharSequenceUtil.isEmpty( ip ) || UNKNOWN_STR.equalsIgnoreCase( ip );
    }

    /**
     * 获取本机的IP地址
     */
    public static String getLocalAddr() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            log.error("InetAddress.getLocalHost()-error", e);
        }
        return "";
    }
}
