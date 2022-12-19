package com.kato.pro.rec.service;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.kato.pro.rec.entity.constant.AbParamConstant;
import com.kato.pro.rec.entity.core.RecommendItem;
import com.kato.pro.rec.entity.po.RecommendRequest;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class StrongPushService {

    @Resource
    private ItemShowedService itemShowedService;

    /**
     * 冷启动，部分用户由于规则，可直接返回定制的数据，毋需推荐
     */
    public List<RecommendItem> tryPush(RecommendRequest recommendRequest) {
        Map<String, String> abMap = recommendRequest.getAbMap();
        String deviceId = recommendRequest.getDeviceId();
        // 2. 强推高热
        List<RecommendItem> directHotList = directHotPush(abMap);
        if (CollUtil.isNotEmpty(directHotList)) { return directHotList; }


        return null;
    }

    /**
     * 根据ab，强推高热
     * @param abMap     abMap
     * @return  List<RecommendResItem>
     */
    private List<RecommendItem> directHotPush(Map<String, String> abMap) {
        String directHotProperty = abMap.getOrDefault(AbParamConstant.DIRECT_PUSH_HOT, "0");
        if (StrUtil.equalsIgnoreCase("0", directHotProperty)) {
            return null;
        }
        // 获取内容

        // 过滤

        // 截取、埋点
        return null;
    }
}
