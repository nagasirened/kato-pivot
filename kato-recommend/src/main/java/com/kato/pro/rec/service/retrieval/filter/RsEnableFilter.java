package com.kato.pro.rec.service.retrieval.filter;

import cn.hutool.core.collection.CollUtil;
import com.kato.pro.rec.entity.core.RsInfo;
import com.kato.pro.rec.entity.enums.RsEnum;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public class RsEnableFilter implements IRetrievalFilter {
    @Override
    public int index() {
        return 1;
    }

    /**
     * 所有的召回源都会枚举出来，如果参数召回源不存在于列表中，或者该召回源不再可用，就过滤该召回源
     * 最后填充一下label和初始化weight权重等信息
     */
    @Override
    public void consume(Set<RsInfo> rsSet, Map<String, String> abMap) {
        if (CollUtil.isEmpty(rsSet)) return;
        Map<String, RsEnum> mapping = Arrays.stream(RsEnum.values())
                .filter(RsEnum::getEnable)
                .collect(Collectors.toMap(RsEnum::getCode, Function.identity()));
        Iterator<RsInfo> iterator = rsSet.iterator();
        while (iterator.hasNext()) {
            RsInfo next = iterator.next();
            RsEnum rsEnum = mapping.get(next.getRsName());
            if (rsEnum == null) {
                iterator.remove();
                continue;
            }
            // 填充label
            next.setLabel(rsEnum.getLabel());
            next.init();
        }
    }
}
