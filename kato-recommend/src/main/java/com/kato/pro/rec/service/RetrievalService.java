package com.kato.pro.rec.service;

import cn.hutool.core.collection.CollUtil;
import com.jd.platform.async.executor.Async;
import com.jd.platform.async.wrapper.WorkerWrapper;
import com.kato.pro.rec.entity.core.RecommendItem;
import com.kato.pro.rec.entity.core.RsInfo;
import com.kato.pro.rec.entity.po.RecommendParams;
import com.kato.pro.rec.service.retrieval.RetrieveStrategyHelper;
import io.micrometer.core.annotation.Timed;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

@Slf4j
@Service
public class RetrievalService {


    @Resource private RetrieveCaptor retrieveCaptor;

    /**
     * 召回数据
     */
    @Timed(histogram = true, percentiles = {0.5, 0.9, 0.99})
    public List<RecommendItem> retrieve(RecommendParams request) {
        Map<String, String> abMap = request.getAbMap();
        // 获取所有的召回源, 并且过滤其中不符合条件的
        List<RsInfo> rsInfos = retrieveCaptor.wrapRecallSources(abMap);
        if (CollUtil.isEmpty(rsInfos)) {
            return new LinkedList<>();
        }
        // 开启多路召回
        WorkerWrapper[] workerWrappers = RetrieveStrategyHelper.allOfAndReturn(rsInfos, request);

        return null;
    }

}
