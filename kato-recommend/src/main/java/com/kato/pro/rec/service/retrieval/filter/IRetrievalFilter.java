package com.kato.pro.rec.service.retrieval.filter;

import com.kato.pro.rec.entity.core.RsInfo;

import java.util.Map;
import java.util.Set;

public interface IRetrievalFilter {
    int index();

    /**
     * 使用迭代器，不满足条件的删除掉
     */
    void consume(Set<RsInfo> rsSet, Map<String, String> abMap);

}
