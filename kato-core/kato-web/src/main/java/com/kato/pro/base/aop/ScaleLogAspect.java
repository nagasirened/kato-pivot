package com.kato.pro.base.aop;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.server.HttpServerRequest;
import com.kato.pro.base.annotation.ScaleLog;
import com.kato.pro.base.log.ScaleLogger;
import com.kato.pro.common.constant.BaseConstant;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;


@Slf4j
@Aspect
@Component
public class ScaleLogAspect {

    @Around("annotation(ScaleLog)")
    public Object scaleLog(ProceedingJoinPoint joinPoint, ScaleLog scaleLog) throws Throwable {
        HttpServerRequest request = (HttpServerRequest) RequestContextHolder.getRequestAttributes();
        if (request == null) {
            return joinPoint.proceed();
        }

        // 动态开启分级日志
        String deviceId = request.getHeader(BaseConstant.DEVICE_ID);
        if (StrUtil.isBlank(deviceId)) {
            return joinPoint.proceed();
        }

        ScaleLogger.init(deviceId);
        try {
            return joinPoint.proceed();
        } finally {
            ScaleLogger.windUp();
        }
    }

}
