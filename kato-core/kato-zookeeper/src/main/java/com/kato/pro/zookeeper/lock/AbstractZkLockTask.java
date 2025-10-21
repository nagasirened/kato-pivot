package com.kato.pro.zookeeper.lock;


import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;

@Slf4j
public abstract class AbstractZkLockTask {

    public ZookeeperSession locker;

    public AbstractZkLockTask(ZookeeperSession locker) {
        this.locker = locker;
    }

    public abstract void execute();

    public void lockAndExecute(String lockPath, int waitTime, TimeUnit timeUnit) {
        boolean locked = false;
        try {
            locked = locker.tryLock(lockPath, waitTime, timeUnit);
            if (locked) {
                log.info("AbstractZkLockTask#tryLock success, lockPath: {}", lockPath);
                execute();
            }
        } catch (Exception e) {
            log.error("AbstractZkLockTask#tryLock error, lockPath: {}, waitTime: {}, timeUnit: {}", lockPath, waitTime, timeUnit, e);
        } finally {
            if (locked) {
                locker.unlock(lockPath);
                log.info("AbstractZkLockTask#unlock success, lockPath: {}", lockPath);
            }
        }
    }

}
