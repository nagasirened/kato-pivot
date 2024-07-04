package com.kato.pro.redisson.lock.realize;

import com.kato.pro.common.constant.BaseConstant;
import com.kato.pro.common.entity.ILock;
import com.kato.pro.common.entity.Locker;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import java.util.concurrent.TimeUnit;

public class RedissonDistributeLock implements ILock<RLock> {

    private final RedissonClient redisson;

    public RedissonDistributeLock(RedissonClient redisson) {
        this.redisson = redisson;
    }

    /**
     * leaseTime
     */
    @Override
    public Locker<RLock> lock(String key, long leaseTime, TimeUnit timeUnit) {
        RLock rLock = redisson.getLock(BaseConstant.LOCK_PREFIX + key);
        rLock.lock(leaseTime, timeUnit);
        return Locker.of(this, rLock);
    }

    @Override
    public Locker<RLock> tryLock(String key, long waitTime, long leaseTime, TimeUnit timeUnit) throws InterruptedException {
        RLock rLock = redisson.getLock(BaseConstant.LOCK_PREFIX + key);
        if (rLock.tryLock(waitTime, leaseTime, timeUnit)) {
            return Locker.of(this, rLock);
        }
        return null;
    }

    @Override
    public void unlock(RLock lockEntity) {
        if (lockEntity == null) return;

        RLock rLock = (RLock) lockEntity;
        if (rLock.isLocked()) {
            rLock.unlock();
        }
    }
}
