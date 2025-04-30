package com.kato.pro.rec.service.retrieval;

import cn.hutool.core.collection.CollUtil;
import com.google.common.collect.Maps;
import com.jd.platform.async.executor.Async;
import com.jd.platform.async.worker.ResultState;
import com.jd.platform.async.worker.WorkResult;
import com.jd.platform.async.wrapper.WorkerWrapper;
import com.kato.pro.base.util.ConfigUtils;
import com.kato.pro.rec.entity.constant.ThreadPoolContainer;
import com.kato.pro.rec.entity.core.RecallBox;
import com.kato.pro.rec.entity.core.RecommendItem;
import com.kato.pro.rec.entity.core.RsInfo;
import com.kato.pro.rec.entity.enums.RsEnum;
import com.kato.pro.rec.entity.po.RecommendParams;
import com.kato.pro.rec.entity.recall.RecallWorker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;

@Slf4j
public class RetrieveStrategyHelper implements CommandLineRunner {

    public static final Map<String, RetrieveStrategy> retrieveBeanMap = Maps.newHashMapWithExpectedSize(RsEnum.values().length);

    @Override
    public void run(String... args) throws Exception {
        Map<String, RetrieveStrategy> strategies = ConfigUtils.getBeanMapByType(RetrieveStrategy.class);
        strategies.forEach((key, value) -> {
            RsEnum rsEnum = value.getRsEnum();
            retrieveBeanMap.put(rsEnum.getRsName(), value);
        });
    }

    public static Map<String, List<RecommendItem>> allOfAndReturn(List<RsInfo> rsInfos, RecommendParams request) {
        WorkerWrapper<RecallBox, List<RecommendItem>>[] workerWrappers = wrapWrappers(rsInfos, request);
        try {
            Async.beginWork(1000L, ThreadPoolContainer.RECALL_POOL, workerWrappers);
        } catch (ExecutionException e) {
            log.error("召回任务出现问题Execution", e);
        } catch (InterruptedException e) {
            log.error("召回任务出现问题Interrupt", e);
            Thread.currentThread().interrupt();
        }
        return parseRecallResult(workerWrappers);
    }

    public static WorkerWrapper[] wrapWrappers(List<RsInfo> rsInfos, RecommendParams request) {
        return rsInfos.stream().map(rs -> {
            RecallBox box = new RecallBox(rs, request);
            RecallWorker recallWorker = new RecallWorker();
            return new WorkerWrapper.Builder<RecallBox, List<RecommendItem>>()
                    .worker(recallWorker)
                    .callback(recallWorker)
                    .param(box)
                    .build();
        }).toArray(WorkerWrapper[]::new);
    }

    private static Map<String, List<RecommendItem>> parseRecallResult(WorkerWrapper<RecallBox, List<RecommendItem>>[] workerWrappers) {
        Map<String, List<RecommendItem>> resultMap = new ConcurrentHashMap<>();
        for (WorkerWrapper<RecallBox, List<RecommendItem>> wrapper : workerWrappers) {
            WorkResult<List<RecommendItem>> workResult = wrapper.getWorkResult();
            if (ResultState.SUCCESS.equals(workResult.getResultState())) {
                List<RecommendItem> recallList = workResult.getResult();
                if (CollUtil.isNotEmpty(recallList)) {
                    resultMap.put(recallList.getFirst().getRs(), recallList);
                }
            }
        }
        return resultMap;
    }

}
