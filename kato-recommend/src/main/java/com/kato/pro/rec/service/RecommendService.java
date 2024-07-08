package com.kato.pro.rec.service;

import cn.hutool.core.collection.CollUtil;
import com.kato.pro.common.utils.JsonUtils;
import com.kato.pro.rec.entity.constant.LogConstant;
import com.kato.pro.rec.entity.core.RecommendItem;
import com.kato.pro.common.entity.LevelEnum;
import com.kato.pro.rec.entity.po.RecommendRequest;
import com.kato.pro.base.util.RateGateway;
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
    public static final String RATE_KEY = "recommend";
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
        RateGateway.tryAcquire(RATE_KEY);
        ScaleLogger.putLog(LogConstant.REQUEST_PARAM, JsonUtils.toJSONString(request), LevelEnum.DETAIL);
        try {
            // 2. 包装需要过滤的数据，如曝光、黑名单等
            personTrashService.wrapExposure(request);
            // 3. 内容冷启动，给予部分特殊情况直接返回某些固定数据池的方式
            List<RecommendItem> directItems = strongPushService.tryPush(request);
            if (CollUtil.isNotEmpty(directItems)) {
                return directItems;
            }
            // 4. 执行推荐
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
        List<RecommendItem> retrieveItems = retrievalService.retrieve(recommendRequest);

        // rank

        // 重排序

        // 后置处理, 如曝光、埋点等

        return null;
    }



}
