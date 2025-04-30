package com.kato.pro.rec.service.retrieval;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.BooleanUtil;
import com.kato.pro.rec.entity.core.RsInfo;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

public interface IRetrievalFilter {
    int index();

    /**
     * 使用迭代器，不满足条件的删除掉
     */
    void consume(Set<RsInfo> rsSet, Map<String, String> abMap, AtomicBoolean suspend);

    /**
     * 过滤方法
     */
    default void sift(Set<RsInfo> rsSet, Map<String, String> abMap, AtomicBoolean suspend) {
        if (CollUtil.isEmpty(rsSet) || BooleanUtil.isTrue(suspend.get())) return;
        consume(rsSet, abMap, suspend);
    }

}
