package com.kato.pro.rec.service;

import cn.hutool.core.text.CharSequenceUtil;
import com.kato.pro.rec.entity.constant.AbParamConstant;
import com.kato.pro.rec.entity.core.RecommendItem;
import com.kato.pro.rec.entity.po.RecommendRequest;
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
     */
    public List<RecommendItem> tryPush(RecommendRequest recommendRequest) {
        Map<String, String> abMap = recommendRequest.getAbMap();
        /*
         * straight push hotItems
         * 1. direct hot
         */
        List<RecommendItem> result = directHotPush(abMap);
        return RecommendUtils.ridItemsOfRes(result, recommendRequest.getExposure());
    }

    /**
     * depends on abMap, push hotItems
     * @param abMap     abMap
     * @return  List<RecommendResItem>
     */
    private List<RecommendItem> directHotPush(Map<String, String> abMap) {
        List<RecommendItem> result = new LinkedList<>();
        String directHotProperty = abMap.getOrDefault(AbParamConstant.DIRECT_PUSH_HOT, "0");
        if (CharSequenceUtil.equalsIgnoreCase("0", directHotProperty)) {
            return result;
        }
        // todo get content

        // filter

        // sub and metrics
        return result;
    }
}
