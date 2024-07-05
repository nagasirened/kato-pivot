package com.kato.pro.rec.service;

import cn.hutool.core.convert.Convert;
import com.kato.pro.base.service.NacosPropertyAcquirer;
import com.kato.pro.common.utils.DateHelper;
import com.kato.pro.base.util.PropertyUtil;
import com.kato.pro.rec.entity.constant.AbOrNacosConstant;
import com.kato.pro.rec.entity.constant.LogConstant;
import com.kato.pro.common.entity.LevelEnum;
import com.kato.pro.rec.entity.po.RecommendRequest;
import com.kato.pro.base.log.ScaleLogger;
import com.kato.pro.rec.utilities.RedisKey;
import com.kato.pro.redis.RedisService;
import io.micrometer.core.annotation.Timed;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
public class RetrievalService {

    @Resource private NacosPropertyAcquirer nacosPropertyAcquirer;
    @Resource private RedisService redisService;

    /**
     * 召回数据
     */
    @Timed(histogram = true, percentiles = {0.5, 0.9, 0.99})
    public void retrieve(RecommendRequest request) {
        // 获取并更新该用户当天推荐次数
        int obtainNumber = refreshObtainNumber(request);


    }

    /**
     * 获取用户当天召回的次数，可能有用
     */
    private int refreshObtainNumber(RecommendRequest request) {
        int res = -1;
        try {
            boolean verifySwitch = PropertyUtil.verifySwitch(nacosPropertyAcquirer, request.getAbMap(), AbOrNacosConstant.OBTAIN_NUMBER, "0");
            if (!verifySwitch) return res;

            String date = DateHelper.currentMMdd();
            String redisKey = RedisKey.RETRIEVE_DEVICE_OBTAIN_NUMBER.makeRedisKey(request.getDeviceId(), date);
            return Convert.toInt(redisService.incr(redisKey, 1L));
        } finally {
            ScaleLogger.putLog(LogConstant.OBTAIN_NUMBER, -1, LevelEnum.NORMAL);
        }
    }

}
