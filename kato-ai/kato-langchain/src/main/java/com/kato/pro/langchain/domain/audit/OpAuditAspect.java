package com.kato.pro.langchain.domain.audit;

import com.kato.pro.langchain.common.security.AuthContext;
import com.kato.pro.langchain.common.security.AuthInfo;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;

/**
 * 操作审计 AOP 拦截器（M11）。
 *
 * 顺序：@Order(10) 在 RoleAspect(0) 之后执行 — 鉴权通过才写审计。
 */
@Slf4j
@Aspect
@Component
@Order(10)
@RequiredArgsConstructor
public class OpAuditAspect {

    private final OpAuditService auditService;

    @Around("@annotation(opAuditLog)")
    public Object audit(ProceedingJoinPoint pjp, OpAuditLog opAuditLog) throws Throwable {
        long start = System.currentTimeMillis();
        Object result = null;
        Integer httpStatus = 200;
        try {
            result = pjp.proceed();
            return result;
        } catch (Throwable t) {
            httpStatus = 500;
            throw t;
        } finally {
            long duration = System.currentTimeMillis() - start;
            try {
                HttpServletRequest req = currentRequest();
                String uri = req != null ? req.getRequestURI() : "<no-req>";
                String method = req != null ? req.getMethod() : "<no-method>";
                String resourceId = extractResourceId(result, pjp);
                auditService.recordFromContext(opAuditLog.action(), opAuditLog.resource(),
                        resourceId, uri, method, httpStatus, pjp.getArgs(), result, duration);
            } catch (Exception e) {
                // 写审计失败不影响主业务
                log.warn("OpAuditAspect record failed", e);
            }
        }
    }

    private static HttpServletRequest currentRequest() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            return attrs == null ? null : attrs.getRequest();
        } catch (Exception e) {
            return null;
        }
    }

    /** 从返回结果中提取 ID（支持 TriggerResponse.runId / 实体.id） */
    private static String extractResourceId(Object result, ProceedingJoinPoint pjp) {
        if (result == null) return null;
        try {
            Method getter = result.getClass().getMethod("getId");
            Object id = getter.invoke(result);
            if (id != null) return id.toString();
        } catch (Exception ignored) {
            // result 类型无 getId — 忽略
        }
        // 退而求其次：取第一个入参的 toString
        Object[] args = pjp.getArgs();
        if (args != null && args.length > 0 && args[0] != null) {
            return args[0].toString();
        }
        return null;
    }
}
