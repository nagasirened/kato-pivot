package com.kato.pro.redisson.lock.aspect;

import cn.hutool.core.text.CharSequenceUtil;
import com.google.common.base.Preconditions;
import com.kato.pro.common.entity.Locker;
import com.kato.pro.redisson.lock.annotation.KatoDistributedLock;
import com.kato.pro.redisson.lock.realize.RedissonDistributeLock;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.redisson.api.RLock;

import javax.annotation.Resource;

@Slf4j
@Aspect
public class KatoLockAspect {

    @Resource
    private RedissonDistributeLock redissonDistributeLock;

    /**
     * 拦截所有注解了 KatoDistributedLock 的内容
     * @param joinPoint                     ProceedingJoinPoint
     * @param katoDistributedLock           锁拓展信息
     * @return  接口正确返回
     */
    @Around("@annotation(katoDistributedLock)")
    public Object around(ProceedingJoinPoint joinPoint, KatoDistributedLock katoDistributedLock) throws Throwable {
        String lockKey = wrapLockKey(katoDistributedLock);
        Locker<RLock> locker = redissonDistributeLock.tryLock(lockKey, katoDistributedLock.expire());
        Preconditions.checkArgument(locker != null, "get lock %s fail ", lockKey);
        log.info("KatoLockAspect#around, get distributeLock {} success, do sth.", lockKey);
        try {
            return joinPoint.proceed();
        } finally {
            locker.close();
            log.info("KatoLockAspect#around, release distributeLock {} success, do sth.", lockKey);
        }
    }

    private String wrapLockKey(KatoDistributedLock katoDistributedLock) {
        String baseName = katoDistributedLock.name();
        String prefix = katoDistributedLock.prefix();
        String suffix = katoDistributedLock.suffix();
        return (CharSequenceUtil.isBlank(prefix) ? baseName : prefix + "_" + baseName) +
                (CharSequenceUtil.isBlank(suffix) ? "" : "_" + suffix);
    }

}
