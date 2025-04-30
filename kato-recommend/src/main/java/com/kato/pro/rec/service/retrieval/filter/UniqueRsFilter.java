package com.kato.pro.rec.service.retrieval.filter;

import com.kato.pro.rec.entity.core.RsInfo;
import com.kato.pro.rec.service.retrieval.IRetrievalFilter;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

public class UniqueRsFilter implements IRetrievalFilter {
    @Override
    public int index() {
        return 3;
    }

    /**
     * 唯一召回源过滤
     * 部分特殊情况只想出某一个召回源的内容，如果存在这个召回源，则将其他的全部过滤掉。
     * 多个唯一召回源的话，label最大的代表最近创建，当出这个
     */
    @Override
    public void consume(Set<RsInfo> rsSet, Map<String, String> abMap, AtomicBoolean suspend) {
        RsInfo unique = rsSet.stream()
                .filter(rs -> Boolean.TRUE.equals(rs.getUnique()))
                .findFirst()
                .orElse(null);
        if (unique != null) {
            Integer label = unique.getLabel();
            rsSet.removeIf(rs -> !rs.getLabel().equals(label));
        }
    }

}
