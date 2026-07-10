package com.kato.pro.langchain.common.security;

import com.kato.pro.langchain.common.exception.BusinessException;
import com.kato.pro.langchain.common.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * 角色校验 AOP 拦截器（M11）。
 *
 * 顺序：必须在 OpAuditAspect 之前执行（鉴权失败不写审计）— @Order(0) < OpAuditAspect(10)
 */
@Slf4j
@Aspect
@Component
@Order(0)
public class RoleAspect {

    @Around("@annotation(requireRole)")
    public Object check(ProceedingJoinPoint pjp, RequireRole requireRole) throws Throwable {
        AuthInfo info = AuthContext.require();
        Role current = info.role();
        for (Role allowed : requireRole.value()) {
            if (allowed == current) {
                return pjp.proceed();
            }
        }
        log.warn("Role check failed: user={} current={} required={}",
                info.username(), current, Arrays.toString(requireRole.value()));
        throw new BusinessException(ErrorCode.FORBIDDEN,
                "需要角色: " + Arrays.toString(requireRole.value()) + "（当前: " + current + "）");
    }
}
