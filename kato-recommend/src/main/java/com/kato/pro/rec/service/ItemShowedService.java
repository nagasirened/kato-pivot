package com.kato.pro.rec.service;

import com.kato.pro.rec.entity.po.RecommendRequest;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;

@Service
public class ItemShowedService {


    /**
     * 获取已经曝光的数据
     */
    public void queryShowedItems(RecommendRequest recommendRequest) {
        String deviceId = recommendRequest.getDeviceId();

        Set<String> itemShowedSet = new HashSet<>();
        // 已经曝光的内容


        // 黑名单内容

        recommendRequest.setItemShowedSet(itemShowedSet);
    }

    private Set<String> getDeviceShowedRecords(String deviceId) {


        return new HashSet<>();
    }

}
