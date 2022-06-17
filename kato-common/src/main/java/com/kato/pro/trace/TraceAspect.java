package com.kato.pro.trace;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.aliyun.oss.ServiceException;
import io.opentracing.Scope;
import io.opentracing.Span;
import io.opentracing.Tracer;
import io.opentracing.tag.Tags;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * 使用前添加注解  @EnableAspectJAutoProxy
 */
@Slf4j
@Aspect
@Order(-1)
public class TraceAspect {

    @Autowired
    private Tracer tracer;

    @Value("{trace.skip.pattern}")
    private Pattern skipPattern;

    /**  controller 方法打点  */
    @Pointcut("execution(* com.kato.pro.controller.*.*(..))")
    public void traceController() {}

    /**  service 方法打点  */
    @Pointcut("execution(* com.kato.pro..impl..*(..))")
    public void traceService() {}

    @Pointcut("bean(cacheService)")
    public void cacheServicePointCut() {}

    @Pointcut("((traceController() || traceService()) && !cacheServicePointCut())")
    public void jaegerPoint() {}

    private final String[] whiteClassArray = new String[] {
            "com.kato.pro.web.BaseInterceptor"
    };
    private final String[] whitePackageArray = new String[] {"com.kato.pro.web", "com.kato.pre.starter"};

    @Around("jaegerPoint()")
    public Object around(ProceedingJoinPoint proceedingJoinPoint) throws Throwable {
        MethodSignature methodSignature = (MethodSignature) proceedingJoinPoint.getSignature();
        String className = methodSignature.getDeclaringTypeName();
        Method method = methodSignature.getMethod();
        String methodName = method.getName();

        // 跳过指定的url
        if (!isTraced(methodName, className, proceedingJoinPoint)) {
            return proceedingJoinPoint.proceed();
        }
        Span span = tracer.buildSpan(className + StrUtil.DOT + methodName).start();
        // 是否打印日志详情
        boolean detail = isDetail(method, className);
        String args = null;
        String result = null;
        Object object;
        try (Scope scope = tracer.scopeManager().activate(span)) {
            args = detail ? "" : jsonPrint(proceedingJoinPoint.getArgs());
            tracer.activeSpan().setTag("methodName", methodName);
            tracer.activeSpan().log("request:" + args);
            tracer.activeSpan().log("thread:" + Thread.currentThread().getName());
            makeUrl(methodName, className, proceedingJoinPoint);
            object = proceedingJoinPoint.proceed();
            result = detail ? "" : jsonPrint(object);
            tracer.activeSpan().log("response:" + result);
        } catch (Throwable t) {
            if (!(t instanceof ServiceException)) {
                log.error("tracer error, className: {}, method: {}, param: {}, result: {}, error message: {}", className, methodName, args, result, t.getMessage(), t);
            }
            Map<String, Object> exceptionLogs = new LinkedHashMap<>(10);
            exceptionLogs.put("event", Tags.ERROR.getKey());
            exceptionLogs.put("request:", args);
            exceptionLogs.put("response:", result);
            exceptionLogs.put("error.object", t);
            span.log(exceptionLogs);
            Tags.ERROR.set(span, true);

            // 错误信息不影响正常的流程处理
            object = proceedingJoinPoint.proceed();
            // throw t;   // 不抛出，正常处理
        } finally {
            span.finish();
        }
        return object;
    }

    private boolean isDetail(Method method, String className) {
        if (method.isAnnotationPresent(RequestMapping.class)) {
            return true;
        }
        if (StrUtil.startWithAny(className, whitePackageArray)) {
            return true;
        }
        return StrUtil.equalsAnyIgnoreCase(className, whiteClassArray);
    }

    /**
     * 返回false代表跳过日志，返回true代表要打印日志
     */
    private boolean isTraced(String methodName, String className, ProceedingJoinPoint proceedingJoinPoint) {
        // 自定义不打印日志的接口
        if (customSkip(methodName, className)) {
            return false;
        }
        // skipPattern如果不存在，则需要日志
        if (Objects.isNull(skipPattern)) {
            return true;
        }
        try {
            HttpServletRequest request = (HttpServletRequest)proceedingJoinPoint.getArgs()[0];
            String url = request.getRequestURI().substring(request.getContextPath().length());
            return !this.skipPattern.matcher(url).matches();
        } catch (Exception e) {
            return true;
        }
    }

    /**
     * 自由拓展要禁止打印的方法
     */
    private boolean customSkip(String methodName, String className) {
        return StrUtil.equalsIgnoreCase(methodName, "doFilter") && StrUtil.equalsIgnoreCase(className, "com.ky.common.web.UserDefinedFilter");
    }

    private void makeUrl(String methodName, String targetName, ProceedingJoinPoint proceedingJoinPoint) {
        try {
            if (!customSkip(methodName, targetName)) {
                return;
            }
            HttpServletRequest request = (HttpServletRequest) proceedingJoinPoint.getArgs()[0];
            String url = request.getRequestURL().toString();
            tracer.activeSpan().log("url:" + url);
        } catch (Exception e) {
        }
    }

    private String jsonPrint(Object arg) {
        try {
            return JSON.toJSONString(arg);
        } catch (Exception e) {
            return "";
        }
    }


}
