package com.kato.pro.rec.service;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.date.DateUtil;
import com.alibaba.fastjson.JSONArray;
import com.kato.pro.base.constant.CommonConstant;
import com.kato.pro.rec.entity.constant.AbOrNacosConstant;
import com.kato.pro.rec.entity.po.RecommendRequest;
import com.kato.pro.base.util.NacosPropertyUtil;
import com.kato.pro.rec.utilities.RedisKey;
import com.kato.pro.redis.RedisService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class PersonTrashService {

    @Resource
    private RedisService redisService;

    @Resource
    private NacosPropertyUtil nacosPropertyUtil;

    /**
     * get data who had been showed, played, and in black-list
     */
    public void wrapTrash(RecommendRequest recommendRequest) {
        String deviceId = recommendRequest.getDeviceId();
        Map<String, String> abMap = recommendRequest.getAbMap();
        Set<Integer> trashSet = recommendRequest.getTrash();
        // showed
        trashSet.addAll(getShowedRecords(deviceId, abMap));
        // played
        trashSet.addAll(getPlayedRecords(deviceId, abMap));
        // black-list
        trashSet.addAll(getBlackRecords(abMap));
    }

    /**
     * get contents who had been showed
     */
    private Set<Integer> getShowedRecords(String deviceId, Map<String, String> abMap) {
        // 1. 当天已曝光数据
        String dated = DateUtil.format(new Date(), CommonConstant.DATE_FORMAT_SIMPLE);
        String redisKey = RedisKey.REC_CONTENT_IMPRESSION.makeRedisKey(dated, deviceId);
        Set<Integer> showSet = Optional.ofNullable(redisService.smembers(redisKey)).orElse(new HashSet<>()).stream().map(Convert::toInt).collect(Collectors.toSet());

        // 2. 过去几天已曝光数据
        Integer days = Convert.toInt(nacosPropertyUtil.getAbOrProperty(abMap, AbOrNacosConstant.RECOMMEND_IMPRESSION_DAYS, "1"));
        if (days <= 1) {
            return showSet;
        }

        try {
            Date today = new Date();
            for (int i = 1; i < days; i++) {
                dated = DateUtil.format(DateUtil.offsetDay(today, -i), CommonConstant.DATE_FORMAT_SIMPLE);
                redisKey = RedisKey.REC_CONTENT_IMPRESSION.makeRedisKey(dated, deviceId);
                showSet.addAll(Optional.ofNullable(redisService.smembers(redisKey)).orElse(new HashSet<>()).stream().map(Convert::toInt).collect(Collectors.toSet()));
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
    private Set<Integer> getPlayedRecords(String deviceId, Map<String, String> abMap) {
        Integer topNumber = Convert.toInt(nacosPropertyUtil.getAbOrProperty(abMap, AbOrNacosConstant.RECOMMEND_PLAYED_NUMBER, "500"));
        String redisKey = RedisKey.REC_CONTENT_PLAYED.makeRedisKey(deviceId);
        Set<String> playedIdSet = Optional.ofNullable(redisService.reverseRangeByScore(redisKey, 0, -1, 0, topNumber)).orElse(new HashSet<>());
        // 已豁免的内容，就算播放过了也可以放出来
        Set<Integer> safetySet = getSafetySet();
        return playedIdSet.stream().map(Convert::toInt)
                .filter(item -> !safetySet.contains(item))
                .collect(Collectors.toSet());
    }

    /** add contents by custom logic */
    private Set<Integer> getSafetySet(/*Map<String, String> abMap*/) {
        return new HashSet<>();
    }

    /**
     * =================================================================
     *     get contents who been in black-list
     * =================================================================
     */
    private Collection<Integer> getBlackRecords(Map<String, String> abMap) {
        Boolean switched = nacosPropertyUtil.checkRule(abMap, AbOrNacosConstant.REC_FILTER_BLACK_SWITCH, "0");
        if (switched) {
            String property = nacosPropertyUtil.getProperty(AbOrNacosConstant.REC_FILTER_BLACK_LIST, "[]");
            return JSONArray.parseArray(property, Integer.class);
        }
        return new LinkedList<>();
    }

}
