package com.kato.pro.rec.entity.constant;

import com.kato.pro.rec.threadPool.MDCWrapperThreadPoolExecutor;
import io.netty.util.concurrent.DefaultThreadFactory;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ThreadPoolContainer {

    public static final ThreadPoolExecutor RECALL_POOL = new MDCWrapperThreadPoolExecutor(
            30, 60, 60, TimeUnit.SECONDS,
            new LinkedBlockingDeque<>(1000),
            new DefaultThreadFactory("recall-source"),
            (task, executor) -> {
                throw new RejectedExecutionException("多路召回RecallSource线程池 Task " + task.toString() + " rejected from " + executor.toString());
            }
    );

}
