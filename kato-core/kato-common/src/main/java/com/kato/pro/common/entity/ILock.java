package com.kato.pro.common.entity;

import java.util.concurrent.TimeUnit;

/**
 * 分布式锁接口类
 */
public interface ILock<T> {

    /**
     * 获取锁, timeUnit=毫秒
     */
    Locker<T> lock(String key, long leaseTime, TimeUnit timeUnit);

    default Locker<T> lock(String key, long leaseTime) {
        return lock(key, leaseTime, TimeUnit.MILLISECONDS);
    }

    default Locker<T> lock(String key) {
        return lock(key, -1);
    }

    /**
     * 尝试获取，没有获取到直接返回
     */
    Locker<T> tryLock(String key, long waitTime, long leaseTime, TimeUnit timeUnit) throws Throwable;


    default Locker<T> tryLock(String key, long waitTime, TimeUnit timeUnit) throws Throwable{
        return tryLock(key, waitTime, -1, timeUnit);
    }

    default Locker<T> tryLock(String key, long waitTime) throws Throwable {
        return tryLock(key, waitTime, TimeUnit.MILLISECONDS);
    }


    /**
     * 释放锁
     */
    void unlock(T lockEntity);

}
