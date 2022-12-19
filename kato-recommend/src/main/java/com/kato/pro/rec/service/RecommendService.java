package com.kato.pro.rec.service;

import cn.hutool.core.collection.CollUtil;
import com.google.common.util.concurrent.RateLimiter;
import com.kato.pro.base.entity.CommonCode;
import com.kato.pro.base.exception.KatoServiceException;
import com.kato.pro.rec.entity.constant.NacosPropertyConstant;
import com.kato.pro.rec.entity.core.RecommendItem;
import com.kato.pro.rec.entity.po.RecommendRequest;
import com.kato.pro.rec.utilities.NacosPropertyUtil;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@SuppressWarnings("all")
public class RecommendService implements CommandLineRunner {

    @Resource NacosPropertyUtil nacosPropertyUtil;
    @Resource StrongPushService strongPushService;
    @Resource ItemShowedService itemShowedService;

    private RateLimiter rateLimiter;

    @Override
    public void run(String... args) throws Exception {
        String permitsPerSecond = nacosPropertyUtil.getProperty(NacosPropertyConstant.RECOMMEND_API_RATE_LIMIT);
        rateLimiter = RateLimiter.create(Integer.parseInt(permitsPerSecond));
    }

    /**
     * 推荐
     * @param request 请求参数
     * @return  List<RecommendResItem>
     */
    public List<RecommendItem> recommend(RecommendRequest recommendRequest) {
        // 1. 预处理
        pretreatment(recommendRequest);
        // 2. 获取已经曝光的内容
        itemShowedService.queryShowedItems(recommendRequest);
        // 3. 冷启动内容直接获取，非推荐直接返回
        List<RecommendItem> coldStartItems = strongPushService.tryPush(recommendRequest);
        if (CollUtil.isNotEmpty(coldStartItems)) { return coldStartItems; }
        // 4. 推荐API
        return doRecommend(recommendRequest);
    }

    /**
     * 推荐预处理
     */
    private void pretreatment(RecommendRequest recommendRequest) {
        // 参数检验
        recommendRequest.check();
        // 单机限流
        if (!rateLimiter.tryAcquire(1)) {
            throw new KatoServiceException(CommonCode.REQUEST_RATE_LIMIT);
        }
    }

    /**
     * 执行推荐
     * @param recommendRequest  请求入参
     * @return  List<RecommendResItem>
     */
    private List<RecommendItem> doRecommend(RecommendRequest recommendRequest) {
        return null;
    }



}
