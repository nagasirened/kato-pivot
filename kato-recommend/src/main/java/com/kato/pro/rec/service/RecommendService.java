package com.kato.pro.rec.service;

import cn.hutool.core.collection.CollUtil;
import com.kato.pro.rec.entity.core.RecommendItem;
import com.kato.pro.rec.entity.enums.LimiterCategory;
import com.kato.pro.rec.entity.po.RecommendRequest;
import com.kato.pro.rec.service.core.LimitationService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Service
@SuppressWarnings("all")
public class RecommendService {

    @Resource private LimitationService limitationService;
    @Resource private StrongPushService strongPushService;
    @Resource private ItemShowedService itemShowedService;

    /**
     * RECOMMEND
     * @param request request-param
     * @return  List<RecommendResItem>
     */
    public List<RecommendItem> recommend(RecommendRequest recommendRequest) {
        // 1. pre
        limitationService.tryAcquire(LimiterCategory.RECOMMEND);
        // 2. 获取已经曝光的内容
        itemShowedService.queryShowedItems(recommendRequest);
        // 3. 冷启动内容直接获取，非推荐直接返回
        List<RecommendItem> coldStartItems = strongPushService.tryPush(recommendRequest);
        if (CollUtil.isNotEmpty(coldStartItems)) { return coldStartItems; }
        // 4. 推荐API
        return doRecommend(recommendRequest);
    }

    /**
     * handle
     * @param recommendRequest  request-param
     * @return  List<RecommendResItem>
     */
    private List<RecommendItem> doRecommend(RecommendRequest recommendRequest) {
        return null;
    }



}
