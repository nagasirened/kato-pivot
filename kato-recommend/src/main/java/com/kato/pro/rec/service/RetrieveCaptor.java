package com.kato.pro.rec.service;

import cn.hutool.core.util.StrUtil;
import com.google.common.base.Splitter;
import com.kato.pro.base.util.ConfigUtils;
import com.kato.pro.common.constant.BaseConstant;
import com.kato.pro.common.utils.JsonUtils;
import com.kato.pro.rec.entity.constant.AbOrNacosConstant;
import com.kato.pro.rec.entity.core.RsInfo;
import lombok.Data;

import java.util.*;

@Data
public class RetrieveCaptor {


    /**
     * 获取召回源：
     * 1. 从abMap中获取；
     * 2. 从配置文件中获取全量的召回源，abMap优先级更高，如果有冲突的以abMap为主
     */
    public void wrapRecallSources(Map<String, String> abMap) {
        String rsStr = abMap.get(AbOrNacosConstant.USER_RECALL_SOURCE);
        Set<RsInfo> rsSet = new HashSet<>();
        // 包装ab
        wrapAbRecallSources(rsStr, rsSet);
        // 包装全量的召回源
        wrapDefaultRecallSources(rsSet);
    }

    // 从ab配置中包装召回源
    private void wrapAbRecallSources(String rsStr, Set<RsInfo> rsSet) {
        if (StrUtil.isBlank(rsStr) || BaseConstant.EMPTY_BRACKET.equals(rsStr)) {
            return;
        }
        rsSet.addAll(Objects.requireNonNull(JsonUtils.toList(rsStr, RsInfo.class)));
    }

    // 从配置中获取
    private void wrapDefaultRecallSources(Set<RsInfo> rsSet) {
        // normal    new,old,cold_start  .....
        String normalRsStr = ConfigUtils.getProperty(AbOrNacosConstant.DEFAULT_NORMAL_RECALL_SOURCES);
        if (StrUtil.isNotBlank(normalRsStr)) {
            Splitter.on(",")
                    .trimResults()
                    .splitToList(normalRsStr)
                    .stream()
                    .map(RsInfo::new)
                    .forEach(rsSet::add);
        }

        // special
        String specialRsStr = ConfigUtils.getProperty(AbOrNacosConstant.DEFAULT_SPECIAL_RECALL_SOURCES);
        if (StrUtil.isNotBlank(specialRsStr) && !BaseConstant.EMPTY_BRACKET.equals(specialRsStr)) {
            rsSet.addAll(Objects.requireNonNull(JsonUtils.toList(specialRsStr, RsInfo.class)));
        }
    }

}
