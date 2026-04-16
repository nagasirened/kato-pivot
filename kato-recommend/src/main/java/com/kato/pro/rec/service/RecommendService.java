package com.kato.pro.rec.service;

import cn.hutool.core.collection.CollUtil;
import com.kato.pro.common.utils.JsonUtils;
import com.kato.pro.rec.entity.constant.LogConstant;
import com.kato.pro.rec.entity.core.RecommendItem;
import com.kato.pro.common.entity.LevelEnum;
import com.kato.pro.rec.entity.po.RecommendParams;
import com.kato.pro.rec.service.rerank.RerankPipelineService;
import com.kato.pro.rec.service.rank.RankService;
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
    @Resource private RerankPipelineService rerankPipelineService;
    @Resource private RankService rankService;

    /**
     * RECOMMEND
     * @param params request-param
     * @return  List<RecommendResItem>
     */
    public List<RecommendItem> recommend(RecommendParams params) {
        // 1. pre
        RateGateway.tryAcquire(RATE_KEY);
        ScaleLogger.putLog(LogConstant.REQUEST_PARAM, JsonUtils.toJSONString(params), LevelEnum.DETAIL);
        try {
            // 2. 包装需要过滤的数据，如曝光、黑名单等
            personTrashService.wrapExposure(params);
            // 3. 内容冷启动，给予部分特殊情况直接返回某些固定数据池的方式
            List<RecommendItem> directItems = strongPushService.tryPush(params);
            if (CollUtil.isNotEmpty(directItems)) {
                return directItems;
            }
            // 4. 执行推荐
            return doRecommend(params);
        } catch (Exception e) {
            log.error("RecommendService#recommend, fail happened, msg: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    /**
     * handle
     * @param recommendParams  request-param
     * @return  List<RecommendResItem>
     */
    private List<RecommendItem> doRecommend(RecommendParams recommendParams) {
        List<RecommendItem> retrieveItems = retrievalService.retrieve(recommendParams);
        Integer topK = recommendParams.getTopK() == null ? 10 : recommendParams.getTopK();
        if (CollUtil.isEmpty(retrieveItems) || retrieveItems.size() < topK) {
            return CollUtil.isEmpty(retrieveItems) ? new ArrayList<>() : new ArrayList<>(retrieveItems);
        }

        // rank（精排模型）：将召回结果送入 TF Serving 进行分数预测，支持 Mock 降级
        List<RecommendItem> ranked = rankService.rank(retrieveItems);

        List<RecommendItem> reranked = rerankPipelineService.rerank(ranked, recommendParams);

        // 后置处理: 写入本次推荐的商品曝光记录到 Redis，供下次推荐时过滤已曝光商品
        personTrashService.recordShowedItems(reranked);

        // TODO: 埋点/日志记录最终推荐结果（如曝光上报、推荐日志打点等），可接入 ScaleLogger 或独立埋点服务
        // doTrackRerankedResult(reranked, recommendParams);

        return reranked;
    }



}
