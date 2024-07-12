package com.kato.pro.rec.service;

import cn.hutool.core.util.StrUtil;
import com.kato.pro.rec.entity.constant.AbOrNacosConstant;
import com.kato.pro.rec.entity.core.RecommendItem;
import com.kato.pro.rec.entity.po.RecommendParams;
import com.kato.pro.rec.utilities.RecommendUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

@Service
public class StrongPushService {

    @Resource
    private PersonTrashService personTrashService;

    /**
     * straight push
     * For some reasons, some users can directly return customized data without recommendation
     * 1. direct hot
     */
    public List<RecommendItem> tryPush(RecommendParams recommendParams) {
        Map<String, String> abMap = recommendParams.getAbMap();
        // 1. direct hot items
        List<RecommendItem> result = directHotPush(abMap);
        return RecommendUtils.ridItemsOfRes(result, recommendParams.getExposure());
    }

    /**
     * depends on abMap, push hotItems
     * @param abMap     abMap
     * @return  List<RecommendResItem>
     */
    private List<RecommendItem> directHotPush(Map<String, String> abMap) {
        List<RecommendItem> result = new LinkedList<>();
        String directHotProperty = abMap.getOrDefault(AbOrNacosConstant.DIRECT_PUSH_HOT, "0");
        if (StrUtil.equalsIgnoreCase("0", directHotProperty)) {
            return result;
        }
        // todo get content

        // filter

        // sub and metrics
        return result;
    }
}
