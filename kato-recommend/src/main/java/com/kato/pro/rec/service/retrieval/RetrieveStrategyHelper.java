package com.kato.pro.rec.service.retrieval;

import cn.hutool.core.collection.CollUtil;
import com.google.common.collect.Maps;
import com.jd.platform.async.executor.Async;
import com.jd.platform.async.worker.ResultState;
import com.jd.platform.async.worker.WorkResult;
import com.jd.platform.async.wrapper.WorkerWrapper;
import com.kato.pro.base.util.ConfigUtils;
import com.kato.pro.common.utils.Lambdas;
import com.kato.pro.rec.entity.constant.RecommendConstant;
import com.kato.pro.rec.entity.core.ThreadPoolContainer;
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
import java.util.function.Function;

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

    public static Map<RsInfo, List<RecommendItem>> allOfAndReturn(List<RsInfo> rsInfos, RecommendParams request) {
        WorkerWrapper<RecallBox, List<RecommendItem>>[] workerWrappers = wrapWrappers(rsInfos, request);
        try {
            Async.beginWork(1000L, ThreadPoolContainer.getThreadPoolExecutor(RecommendConstant.THREAD_POOL_RECALL), workerWrappers);
        } catch (ExecutionException e) {
            log.error("召回任务出现问题Execution", e);
        } catch (InterruptedException e) {
            log.error("召回任务出现问题Interrupt", e);
            Thread.currentThread().interrupt();
        }
        return parseRecallResult(rsInfos, workerWrappers);
    }

    /**
     * 包装多路召回任务
     */
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

    /**
     * 解析多路召回的结果
     */
    private static Map<RsInfo, List<RecommendItem>> parseRecallResult(List<RsInfo> rsInfos, WorkerWrapper<RecallBox, List<RecommendItem>>[] workerWrappers) {
        Map<String, RsInfo> rsMap = Lambdas.toMap(rsInfos, RsInfo::getRsName, Function.identity());
        Map<RsInfo, List<RecommendItem>> resultMap = new ConcurrentHashMap<>();
        for (WorkerWrapper<RecallBox, List<RecommendItem>> wrapper : workerWrappers) {
            WorkResult<List<RecommendItem>> workResult = wrapper.getWorkResult();
            if (ResultState.SUCCESS.equals(workResult.getResultState())) {
                List<RecommendItem> recallList = workResult.getResult();
                if (CollUtil.isNotEmpty(recallList)) {
                    resultMap.put(rsMap.get(recallList.get(0).getRs()), recallList);
                }
            }
        }
        return resultMap;
    }

}
