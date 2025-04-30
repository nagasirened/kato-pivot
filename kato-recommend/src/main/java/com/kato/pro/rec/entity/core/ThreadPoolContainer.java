package com.kato.pro.rec.entity.core;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.text.StrPool;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.kato.pro.base.util.ConfigUtils;
import com.kato.pro.rec.threadPool.MDCWrapperThreadPoolExecutor;
import io.netty.util.concurrent.DefaultThreadFactory;
import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.*;

@Data
@Component
@FieldNameConstants
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ThreadPoolContainer implements CommandLineRunner {

    public static final String PREFIX = "thread.pool.";
    public static final Map<String, ThreadPoolExecutor> threadPoolMap = new HashMap<>();

    /**
     * 项目启动加载线程池数据
     */
    @Override
    public void run(String... args) throws Exception {
        String poolNameStr = ConfigUtils.getProperty(PREFIX + "types");
        if (CharSequenceUtil.isBlank(poolNameStr)) {
            return;
        }
        Splitter.on(",")
                .trimResults()
                .splitToList(poolNameStr)
                .forEach(poolName -> {
                    ThreadPoolExecutor threadPoolExecutor = wrapExecutor(poolName);
                    threadPoolMap.put(poolName, threadPoolExecutor);
                });
    }

    public static ThreadPoolExecutor getThreadPoolExecutor(String threadPoolName) {
        ThreadPoolExecutor threadPoolExecutor = threadPoolMap.get(threadPoolName);
        Preconditions.checkArgument(Objects.nonNull(threadPoolExecutor), threadPoolName + " pool is not exist");
        return threadPoolExecutor;
    }

    /**
     * 刷新线程池核心数，提供的接口调用
     */
    public void refresh(String poolName) {
        ThreadPoolExecutor threadPoolExecutor = threadPoolMap.get(poolName);
        if (Objects.isNull(threadPoolExecutor)) {
            String poolNameStr = ConfigUtils.getProperty(PREFIX + "types");
            if (poolNameStr.contains(poolName)) {
                threadPoolMap.put(poolName, wrapExecutor(poolName));
            }
            return;
        }
        int corePoolSize = threadPoolExecutor.getCorePoolSize();
        Integer currentConfigCoreSize = fetchCoreSize(poolName);
        if (currentConfigCoreSize != corePoolSize) {
            threadPoolExecutor.setCorePoolSize(currentConfigCoreSize);
        }
    }

    /**
     * 创建线程池
     */
    public ThreadPoolExecutor wrapExecutor(String threadPoolName) {
        Integer coreSize = fetchCoreSize(threadPoolName);
        Integer dequeSize = fetchDequeSize(threadPoolName);
        return new MDCWrapperThreadPoolExecutor(
                coreSize, coreSize, 60, TimeUnit.SECONDS,
                new LinkedBlockingDeque<>(dequeSize),
                new DefaultThreadFactory(threadPoolName),
                (task, executor) -> {
                    throw new RejectedExecutionException(threadPoolName + "线程池 Task " + task.toString() + " rejected from " + executor.toString());
                }
        );
    }

    /**
     * 动态获取线程池核心数。这个配置是必须的，且配置满足 thread.pool.{poolName}.coreSize
     */
    public Integer fetchCoreSize(String poolName) {
        String property = ConfigUtils.getProperty(PREFIX + poolName + StrPool.DOT + "coreSize");
        if (CharSequenceUtil.isBlank(property)) {
            throw new IllegalStateException( poolName + " has no coreSize config");
        }
        return Convert.toInt(property);
    }

    /**
     * 动态获取线程池核心数。这个配置不配置默认长度是1000，且配置满足 thread.pool.{poolName}.dequeSize
     */
    public Integer fetchDequeSize(String poolName) {
        String property = ConfigUtils.getProperty(PREFIX + poolName + StrPool.DOT + "dequeSize", "1000");
        return Convert.toInt(property);
    }

}
