package com.kato.pro.auth.aspect;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.kato.pro.auth.annotation.PreAuth;
import com.kato.pro.base.constant.OAuth2Constant;
import com.kato.pro.base.entity.LoginUser;
import com.kato.pro.base.exception.TokenException;
import com.kato.pro.redis.RedisService;
import com.kato.pro.base.util.SecurityUtil;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.util.PatternMatchUtils;
import org.springframework.web.context.request.RequestContextHolder;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;


@Slf4j
@Aspect
public class PreAuthAspect {

    /**
     * 权限标识
     */
    public static final String ALL_PERMISSIONS = "*:*:*";

    private final RedisService redisService;
    public PreAuthAspect(RedisService redisService) {
        this.redisService = redisService;
    }

    @SneakyThrows
    @Around("@annotation(com.kato.pro.auth.annotation.PreAuth)")
    public Object around(ProceedingJoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature)joinPoint.getSignature();
        Method method = signature.getMethod();
        PreAuth preAuth = method.getAnnotation(PreAuth.class);
        if (ObjectUtil.isNull(preAuth) || !preAuth.enable()) {
            return joinPoint.proceed();
        }
        HttpServletRequest request = (HttpServletRequest) RequestContextHolder.getRequestAttributes();
        if (verifyPermission(preAuth.hasPerm(), request)) {
            return joinPoint.proceed();
        }
        throw new TokenException("无权进行操作");
    }

    public boolean verifyPermission(String permission, HttpServletRequest request) {
        LoginUser loginUser = SecurityUtil.getLoginUser(request);
        if (ObjectUtil.isNull(loginUser)) {
            return false;
        }
        if (StrUtil.isBlank(permission)) {
            return true;
        }
        /* 超级管理员直接跳过 */
        if (StrUtil.equalsIgnoreCase(loginUser.getAccount(), OAuth2Constant.SUPER_ADMIN)) {
            return true;
        }
        String redisKey = OAuth2Constant.PERMISSION_PREFIX + loginUser.getAccount() + StrUtil.DOT + loginUser.getRoleId();
        String permissionData = Convert.toStr(redisService.get(redisKey));
        try {
            return hasPermissions(JSONObject.parseObject(permissionData, new TypeReference<List<String>>() {}), permission);
        } catch (Exception ignore) {
            log.warn("PreAuthAspect#verifyPermission");
            return false;
        }
    }

    private boolean hasPermissions(Collection<String> authorityList, String permission) {
        return authorityList.stream()
                .filter(StrUtil::isNotBlank)
                .anyMatch(x -> ALL_PERMISSIONS.contains(x) || PatternMatchUtils.simpleMatch(permission, x));

    }


}
