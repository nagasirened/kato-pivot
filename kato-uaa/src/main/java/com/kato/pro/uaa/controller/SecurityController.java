package com.kato.pro.uaa.controller;

import cn.hutool.core.text.CharSequenceUtil;
import com.kato.pro.uaa.properties.SecurityProperties;
import com.kato.pro.base.entity.Result;
import com.kato.pro.common.constant.CommonCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.web.DefaultRedirectStrategy;
import org.springframework.security.web.RedirectStrategy;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.RequestCache;
import org.springframework.security.web.savedrequest.SavedRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Slf4j
@RestController
@RequestMapping("/authentication")
public class SecurityController {

    @Resource private SecurityProperties securityProperties;

    private final RequestCache requestCache = new HttpSessionRequestCache();
    private final RedirectStrategy redirectStrategy = new DefaultRedirectStrategy();

    @PostMapping("/require")
    public Result<String> requireLogin(HttpServletRequest request, HttpServletResponse response) throws IOException {
        // session缓存的请求
        SavedRequest savedRequest = requestCache.getRequest(request, response);
        if (savedRequest != null) {
            String targetUrl = savedRequest.getRedirectUrl();
            log.info("SecurityController#requireLogin, the direct targetUrl: {}", targetUrl);
            if (CharSequenceUtil.endWithIgnoreCase(targetUrl, ".html")) {
                redirectStrategy.sendRedirect(request, response, securityProperties.getBrowser().getLoginPage());
            }
        }
        return Result.build(CommonCode.SECURITY_REQUIRE);
    }

}
