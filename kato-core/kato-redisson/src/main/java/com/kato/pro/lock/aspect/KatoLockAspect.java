package com.kato.pro.lock.aspect;

import cn.hutool.core.util.StrUtil;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.kato.pro.lock.annotation.KatoDistributedLock;
import com.kato.pro.lock.realize.KatoRedissonLock;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Slf4j
@Aspect
@Component
public class KatoLockAspect {

    @Resource
    private KatoRedissonLock katoRedissonLock;

    /**
     * 拦截所有注解了 KatoDistributedLock 的内容
     * @param joinPoint                     ProceedingJoinPoint
     * @param katoDistributedLock           锁拓展信息
     * @return
     */
    @Around("@annotation(katoDistributedLock)")
    public Object around(ProceedingJoinPoint joinPoint, KatoDistributedLock katoDistributedLock) throws Throwable {
        String lockKey = wrapLockKey(katoDistributedLock);
        boolean getLock = katoRedissonLock.tryLock(lockKey, katoDistributedLock.expire());
        Preconditions.checkArgument(getLock, "get lock %s fail ", lockKey);
        log.info("KatoLockAspect#around, get distributeLock {} success, do sth.", lockKey);
        try {
            return joinPoint.proceed();
        } finally {
            katoRedissonLock.releaseLock(lockKey);
            log.info("KatoLockAspect#around, release distributeLock {} success, do sth.", lockKey);
        }
    }

    private String wrapLockKey(KatoDistributedLock katoDistributedLock) {
        String baseName = katoDistributedLock.name();
        String prefix = katoDistributedLock.prefix();
        String suffix = katoDistributedLock.suffix();
        return (StrUtil.isBlank(prefix) ? baseName : prefix + "_" + baseName) +
                (StrUtil.isBlank(suffix) ? "" : "_" + suffix);
    }
}
