package com.kato.pro.rec.service;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.convert.Convert;
import com.kato.pro.base.util.ConfigUtils;
import com.kato.pro.common.resolver.DeviceContextHolder;
import com.kato.pro.common.utils.DateHelper;
import com.kato.pro.rec.entity.constant.AbOrNacosConstant;
import com.kato.pro.rec.entity.constant.LogConstant;
import com.kato.pro.common.entity.LevelEnum;
import com.kato.pro.rec.entity.core.RecommendItem;
import com.kato.pro.rec.entity.core.RsInfo;
import com.kato.pro.rec.entity.po.RecommendParams;
import com.kato.pro.base.log.ScaleLogger;
import com.kato.pro.rec.service.core.CacheService;
import com.kato.pro.rec.utilities.RedisKey;
import com.kato.pro.redis.RedisService;
import io.micrometer.core.annotation.Timed;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

@Service
public class RetrievalService {

    @Resource private RetrieveCaptor retrieveCaptor;

    /**
     * 召回数据
     */
    @Timed(histogram = true, percentiles = {0.5, 0.9, 0.99})
    public List<RecommendItem> retrieve(RecommendParams params) {
        Map<String, String> abMap = params.getAbMap();
        // 获取所有的召回源, 并且过滤其中不符合条件的
        List<RsInfo> rsInfos = retrieveCaptor.wrapRecallSources(abMap);
        if (CollUtil.isEmpty(rsInfos)) {
            return new LinkedList<>();
        }
        // 开启多路召回

        return null;
    }


}
