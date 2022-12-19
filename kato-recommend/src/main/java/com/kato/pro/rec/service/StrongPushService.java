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
     * straight push
     * For some reasons, some users can directly return customized data without recommendation
     */
    public List<RecommendItem> tryPush(RecommendRequest recommendRequest) {
        Map<String, String> abMap = recommendRequest.getAbMap();
        String deviceId = recommendRequest.getDeviceId();
        Set<String> itemShowedSet = recommendRequest.getItemShowedSet();
        // 2. straight push hotItems
        List<RecommendItem> directHotList = directHotPush(abMap);
        if (CollUtil.isNotEmpty(directHotList)) { return directHotList; }


        return null;
    }

    /**
     * depends on abMap, push hotItems
     * @param abMap     abMap
     * @return  List<RecommendResItem>
     */
    private List<RecommendItem> directHotPush(Map<String, String> abMap) {
        String directHotProperty = abMap.getOrDefault(AbParamConstant.DIRECT_PUSH_HOT, "0");
        if (StrUtil.equalsIgnoreCase("0", directHotProperty)) {
            return null;
        }
        // get content

        // filter

        // sub and metrics
        return null;
    }
}
