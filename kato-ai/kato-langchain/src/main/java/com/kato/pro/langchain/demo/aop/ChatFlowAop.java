package com.kato.pro.langchain.demo.aop;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@Aspect
public class ChatFlowAop {

    private static final String USER_ROLE = "sessionId";
    private static final String AI_RILE = "message";

    /**
     * 利用AOP拦截用户信息，只记录用户输入和最终输出到日志中
     */
    @Around("@annotation(ChatFlow)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        Object[] args = joinPoint.getArgs();
        String sessionId = (String) args[0];
        String message = (String) args[1];
        log.info("sessionId: {}, user message: {}", sessionId, message);
        // todo 记录表
        Object resulObject = joinPoint.proceed();

        log.info("sessionId: {}, ai message: {}", sessionId, message);
        // todo 记录表
        return resulObject;
    }

}
