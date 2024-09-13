package com.kato.pro.rec.service.retrieval.filter;

import cn.hutool.core.collection.CollUtil;
import com.kato.pro.rec.entity.core.RsInfo;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.reflections.Reflections;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class RetrievalFilterProcessor {

    public static final List<IRetrievalFilter> filters = new LinkedList<>();

    public static void main(String[] args) {
        RetrievalFilterProcessor.loadFilters();
    }

    /**
     * 加载所有检索过滤器
     */
    public static void loadFilters() {
        Reflections reflections = new Reflections("com.kato.pro.rec.service.retrieval.filter");
        Set<Class<? extends IRetrievalFilter>> implementations = reflections.getSubTypesOf(IRetrievalFilter.class);
        for (Class<? extends IRetrievalFilter> implClass : implementations) {
            try {
                filters.add(implClass.getDeclaredConstructor().newInstance());
            } catch (Exception e) {
                log.error("load retrievalFilter fail:{}", implClass.getName(), e);;
            }
        }

    }

    /**
     * 顺序过滤
     */
    public static void doFilter(Set<RsInfo> rsInfoList, Map<String, String> abMap) {
        if (CollUtil.isEmpty(filters)) return;
        filters.forEach(filter -> filter.consume(rsInfoList, abMap));
    }


}
