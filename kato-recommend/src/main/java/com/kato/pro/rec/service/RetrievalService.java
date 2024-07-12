package com.kato.pro.rec.service;

import cn.hutool.core.convert.Convert;
import com.kato.pro.base.util.ConfigUtils;
import com.kato.pro.common.utils.DateHelper;
import com.kato.pro.rec.entity.constant.AbOrNacosConstant;
import com.kato.pro.rec.entity.constant.LogConstant;
import com.kato.pro.common.entity.LevelEnum;
import com.kato.pro.rec.entity.core.RecommendItem;
import com.kato.pro.rec.entity.po.RecommendParams;
import com.kato.pro.base.log.ScaleLogger;
import com.kato.pro.rec.utilities.RedisKey;
import com.kato.pro.redis.RedisService;
import io.micrometer.core.annotation.Timed;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Service
public class RetrievalService {

    @Resource private RedisService redisService;
    @Resource private RetrieveCaptor retrieveCaptor;

    /**
     * 召回数据
     */
    @Timed(histogram = true, percentiles = {0.5, 0.9, 0.99})
    public List<RecommendItem> retrieve(RecommendParams request) {
        // 获取并更新该用户当天推荐次数
        int obtainNumber = refreshObtainNumber(request);
        // 获取所有的召回源, 并且过滤其中不符合条件的
        retrieveCaptor.wrapRecallSources(request.getAbMap());

        return null;
    }

    /**
     * 获取用户当天召回的次数，可能有用. 没有打开开关的话默认为-1
     */
    private int refreshObtainNumber(RecommendParams request) {
        int res = -1;
        try {
            boolean verifySwitch = ConfigUtils.checkRule(request.getAbMap(), AbOrNacosConstant.OBTAIN_NUMBER);
            if (!verifySwitch) return res;

            String date = DateHelper.currentMMdd();
            String redisKey = RedisKey.RETRIEVE_DEVICE_OBTAIN_NUMBER.makeRedisKey(request.getDeviceId(), date);
            return Convert.toInt(redisService.incr(redisKey, 1L));
        } finally {
            ScaleLogger.putLog(LogConstant.OBTAIN_NUMBER, -1, LevelEnum.NORMAL);
        }
    }

}
