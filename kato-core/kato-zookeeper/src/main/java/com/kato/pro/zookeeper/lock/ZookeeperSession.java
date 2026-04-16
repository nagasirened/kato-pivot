package com.kato.pro.zookeeper.lock;

import com.kato.pro.zookeeper.config.property.BaseZkProperties;
import lombok.extern.slf4j.Slf4j;
import org.apache.zookeeper.*;
import org.apache.zookeeper.Watcher.Event.KeeperState;
import org.apache.zookeeper.data.Stat;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@Slf4j
public class ZookeeperSession implements Watcher {
    public static final String PREFIX = "/zkLock-";

    public final static CountDownLatch connectTrafficLight = new CountDownLatch(1);
    private final String lockBasePath;
    private final ZooKeeper zookeeper;
    public CountDownLatch latch;

    public ZookeeperSession(BaseZkProperties properties) throws Exception {
        this.lockBasePath = properties.getLockBasePath();

        this.zookeeper = new ZooKeeper(properties.getAddress(), properties.getSessionTimeout(), this/* watch process函数 */);
        try {
            connectTrafficLight.await();
            log.info("zookeeper session established......");
            ensureBasePath();
        } catch (InterruptedException e) {
            log.error("zookeeper session setup interrupt......");
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 确保基础路径存在
     */
    private void ensureBasePath() {
        try {
            Stat stat = zookeeper.exists(lockBasePath, false);
            if (stat == null) {
                zookeeper.create(lockBasePath, new byte[0], ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);  // 永久存在
            }
        } catch (KeeperException.NodeExistsException e) {
            // 节点已存在，忽略
        } catch (Exception e) {
            throw new RuntimeException("Failed to create base path: " + lockBasePath, e);
        }
    }

    /**
     * 尝试获取锁（带等待时间）
     * @param waitTime 最大等待时间
     * @param timeUnit 时间单位
     * @return 是否成功获取锁
     */
    public boolean tryLock(String lockName, long waitTime, TimeUnit timeUnit) throws Exception {
        long startTime = System.currentTimeMillis();
        long maxWaitMillis = timeUnit.toMillis(waitTime);

        // 创建临时顺序节点
        // /base/lock-224-0000000001
        // /base/lock-224-0000000002
        // /base/lock-224-0000000003 ...

        String lockPath = lockBasePath + PREFIX + lockName + "-";
        String currentLockPath = zookeeper.create(lockPath, new byte[0],
                ZooDefs.Ids.OPEN_ACL_UNSAFE,
                CreateMode.EPHEMERAL_SEQUENTIAL);

        while (true) {
            // 获取所有锁节点并排序  ["lock-224-0000000001", "lock-224-0000000002", "lock-224-0000000003"]
            List<String> lockNodes = zookeeper.getChildren(lockBasePath, false);
            Collections.sort(lockNodes);

            String smallestNode = lockNodes.getFirst();
            String currentSequence = currentLockPath.substring(lockBasePath.length() + PREFIX.length());

            // 如果当前节点是最小的节点，则获得锁
            if (smallestNode.equals(currentSequence)) {
                return true;
            }

            // 检查是否超时 ｜ 超时，删除当前节点并返回失败
            if (System.currentTimeMillis() - startTime > maxWaitMillis) {
                unlock(currentLockPath);
                return false;
            }

            // 找到前一个节点并监听它
            int currentIndex = lockNodes.indexOf(currentSequence);
            String previousNode = lockNodes.get(currentIndex - 1);
            String previousNodePath = lockBasePath + "/" + previousNode;
            // 等排序在前面的那个节点，如果存在则Stat有值，会等一段时间到最多等的时间；
            // 如果前面的节点已经不存在了，stat就是null了，直接进入下一次循环
            final CountDownLatch latch = new CountDownLatch(1);
            Stat stat = zookeeper.exists(previousNodePath, new Watcher() {
                @Override
                public void process(WatchedEvent event) {
                    if (event.getType() == Event.EventType.NodeDeleted) {
                        latch.countDown();
                    }
                }
            });

            // 如果前一个节点仍然存在，等待它被删除
            if (stat != null) {
                long remainingTime = maxWaitMillis - (System.currentTimeMillis() - startTime);
                if (remainingTime > 0) {
                    latch.await(remainingTime, TimeUnit.MILLISECONDS);
                } else {
                    // 剩余时间不足，解锁并返回失败
                    unlock(currentLockPath);
                    return false;
                }
            }
        }
    }


    /**
     * 释放锁
     */
    public void unlock(String currentLockPath) {
        if (currentLockPath != null) {
            try {
                zookeeper.delete(currentLockPath, -1);
            } catch (Exception e) {
                // 记录日志或处理异常
                log.error("Failed to delete lock node: {}", currentLockPath, e);
            }
        }
    }

    /**
     * 建立 zk session 的 watcher
     */
    public void process(WatchedEvent event) {
        System.out.println("Receive watched event: " + event.getState());

        if (KeeperState.SyncConnected == event.getState()) {
            connectTrafficLight.countDown();
        }

        if (this.latch != null) {
            this.latch.countDown();
        }
    }


}
