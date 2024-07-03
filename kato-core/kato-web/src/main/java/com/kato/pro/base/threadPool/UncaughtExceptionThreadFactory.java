package com.kato.pro.base.threadPool;

import cn.hutool.core.lang.Assert;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ThreadFactory;

/**
 * 线程池线程如果发生错误，捕获异常并打印日志
 */
@Slf4j
public class UncaughtExceptionThreadFactory implements ThreadFactory {

    @Override
    public Thread newThread(Runnable r) {
        Assert.notNull(r, "runnable is null");
        Thread thread = new Thread(r);
        thread.setUncaughtExceptionHandler((t, e) ->
            log.error("{} error happened, msg: {}", t.getName(), e.getMessage(), e)
        );
        return thread;
    }

}
