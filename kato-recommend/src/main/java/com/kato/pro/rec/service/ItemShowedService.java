package com.kato.pro.rec.service;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.date.DateUtil;
import com.alibaba.fastjson.JSONArray;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.kato.pro.base.constant.CommonConstant;
import com.kato.pro.rec.entity.constant.AbOrNacosConstant;
import com.kato.pro.rec.entity.po.RecommendRequest;
import com.kato.pro.rec.utilities.NacosPropertyUtil;
import com.kato.pro.rec.utilities.RedisKey;
import com.kato.pro.redis.RedisService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class ItemShowedService {

    @Resource
    private RedisService redisService;

    @Resource
    private NacosPropertyUtil nacosPropertyUtil;

    /**
     * get data who had been showed, played, and in black-list
     */
    public void queryShowedItems(RecommendRequest recommendRequest) {
        String deviceId = recommendRequest.getDeviceId();
        Map<String, String> abMap = recommendRequest.getAbMap();
        Set<String> itemShowedSet = recommendRequest.getItemShowedSet();
        if (Objects.isNull(itemShowedSet)) {
            itemShowedSet = new HashSet<>();
        }
        // showed
        itemShowedSet.addAll(getShowedRecords(deviceId, abMap));
        // played
        itemShowedSet.addAll(getPlayedRecords(deviceId, abMap));
        // black-list
        itemShowedSet.addAll(getBlackRecords(abMap));
    }

    /**
     * =================================================================
     *                 get contents who had been showed
     * =================================================================
     */
    @SuppressWarnings("all")
    final LoadingCache<String, Set<String>> contentImpressionCache = CacheBuilder.newBuilder()
            .initialCapacity(64000)
            .maximumSize(320000)
            .refreshAfterWrite(15, TimeUnit.MINUTES)
            .expireAfterAccess(30, TimeUnit.MINUTES)
            .build(new CacheLoader<String, Set<String>>() {
                public Set<String> load(String redisKey) {
                    return Optional.ofNullable(redisService.smembers(redisKey)).orElse(new HashSet<>());
                }
            });

    /**
     * get contents who had been showed
     */
    private Set<String> getShowedRecords(String deviceId, Map<String, String> abMap) {
        // 1. 当天已曝光数据
        String dated = DateUtil.format(new Date(), CommonConstant.DATEFORMAT_SIMPLE);
        String redisKey = RedisKey.REC_CONTENT_IMPRESSION.makeRedisKey(dated, deviceId);
        Set<String> showSet = Optional.ofNullable(redisService.smembers(redisKey)).orElse(new HashSet<>());

        // 2. 过去几天已曝光数据
        Integer days = Convert.toInt(nacosPropertyUtil.getAbOrProperty(abMap, AbOrNacosConstant.RECOMMEND_IMPRESSION_DAYS, "1"));
        if (days <= 1) {
            return showSet;
        }

        try {
            for (int i = 1; i < days; i++) {
                dated = DateUtil.format(DateUtil.offsetDay(new Date(), -1), CommonConstant.DATEFORMAT_SIMPLE);
                redisKey = RedisKey.REC_CONTENT_IMPRESSION.makeRedisKey(dated, deviceId);
                showSet.addAll(contentImpressionCache.get(redisKey));
            }
        } catch (Exception e) {
            log.error("QueryImpression of last-days failed, device: {}, msg: {}", deviceId, e.getMessage(), e);
        }
        return showSet;
    }

    /**
     * =================================================================
     *      get contents who had been played todo spark remove spare data in z-set
     * =================================================================
     */
    private Set<String> getPlayedRecords(String deviceId, Map<String, String> abMap) {
        Integer topNumber = Convert.toInt(nacosPropertyUtil.getAbOrProperty(abMap, "rec.readings.number", "500"));
        String redisKey = RedisKey.REC_CONTENT_PLAYED.makeRedisKey(deviceId);
        Set<String> playedIdSet = Optional.ofNullable(redisService.reverseRangeByScore(redisKey, 0, -1, 0, topNumber)).orElse(new HashSet<>());
        // 已豁免的内容
        HashSet<Object> safetySet = getSafetySet(abMap);
        playedIdSet.removeIf(safetySet::contains);
        return playedIdSet;
    }

    /** add contents by custom logic */
    private HashSet<Object> getSafetySet(Map<String, String> abMap) {
        return new HashSet<>();
    }

    /**
     * =================================================================
     *     get contents who been in black-list
     * =================================================================
     */
    private Collection<String> getBlackRecords(Map<String, String> abMap) {
        Boolean switched = nacosPropertyUtil.checkRule(abMap, AbOrNacosConstant.REC_FILTER_BLACK_SWITCH, "0");
        if (switched) {
            String property = nacosPropertyUtil.getProperty(AbOrNacosConstant.REC_FILTER_BLACK_LIST, "[]");
            return JSONArray.parseArray(property, String.class);
        }
        return new ArrayList<>(0);
    }

}
