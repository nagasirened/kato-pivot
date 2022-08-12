package com.kato.pro.lock.realize;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import java.util.concurrent.TimeUnit;

@Slf4j
public class KatoRedissonLock {

    @Getter
    private final RedissonClient redissonClient;

    public KatoRedissonLock(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    /**
     * 加锁
     */
    public boolean tryLock(String lockKey, int expire) {
        RLock rLock = redissonClient.getLock(lockKey);
        try {
            if (rLock.tryLock(expire, TimeUnit.SECONDS)) {
                log.info("RedissonLock#tryLock, lock success, key is {}", lockKey);
                return true;
            } else {
                log.info("RedissonLock#tryLock, lock fail, key is {}", lockKey);
            }
        } catch (Exception e) {
            log.error("RedissonLock#tryLock, wrong occurred during the locking process.", e);
        }
        return false;
    }

    /**
     * 释放锁
     * @param lockKey
     */
    public void releaseLock(String lockKey) {
        redissonClient.getLock(lockKey).unlock();
    }


}
