package com.kato.pro.rec.service;

import cn.hutool.core.collection.CollUtil;
import com.kato.pro.common.utils.JsonUtils;
import com.kato.pro.rec.entity.constant.LogConstant;
import com.kato.pro.rec.entity.core.RecommendItem;
import com.kato.pro.common.entity.LevelEnum;
import com.kato.pro.rec.entity.enums.LimiterCategory;
import com.kato.pro.rec.entity.po.RecommendRequest;
import com.kato.pro.rec.service.core.RateGateway;
import com.kato.pro.base.log.ScaleLogger;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@SuppressWarnings("all")
public class RecommendService {

    @Resource private RateGateway rateGateway;
    @Resource private StrongPushService strongPushService;
    @Resource private PersonTrashService personTrashService;
    @Resource private RetrievalService retrievalService;

    /**
     * RECOMMEND
     * @param request request-param
     * @return  List<RecommendResItem>
     */
    public List<RecommendItem> recommend(RecommendRequest request) {
        // 1. pre
        rateGateway.tryAcquire(LimiterCategory.RECOMMEND);
        try {
            // log init
            ScaleLogger.putLog(LogConstant.REQUEST_PARAM, JsonUtils.toJSONString(request), LevelEnum.DETAIL);
            // 2. query items which need to filter
            personTrashService.wrapTrash(request);
            // 3. cold_start, which contains content will return value directly
            List<RecommendItem> coldStartItems = strongPushService.tryPush(request);
            if (CollUtil.isNotEmpty(coldStartItems)) { return coldStartItems; }
            // 4. handle recommend
            return doRecommend(request);
        } catch (Exception e) {
            log.error("RecommendService#recommend, fail happened, msg: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    /**
     * handle
     * @param recommendRequest  request-param
     * @return  List<RecommendResItem>
     */
    private List<RecommendItem> doRecommend(RecommendRequest recommendRequest) {
        // 召回/裁剪
        retrievalService.retrieve(recommendRequest);

        // rank

        // 重排序

        // 后置处理

        return null;
    }



}
