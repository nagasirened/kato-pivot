package com.kato.pro.rec.service.retrieval;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.text.StrPool;
import com.kato.pro.rec.entity.core.RsInfo;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.reflections.Reflections;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Component
public class RetrievalFilterProcessor implements BeanPostProcessor {

    public static final List<IRetrievalFilter> filters = new LinkedList<>();

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) {
        String packageName = bean.getClass().getPackage().getName();
        Reflections reflections = new Reflections(packageName + StrPool.DOT + "filter");
        Set<Class<? extends IRetrievalFilter>> implementations = reflections.getSubTypesOf(IRetrievalFilter.class);
        for (Class<? extends IRetrievalFilter> implClass : implementations) {
            try {
                filters.add(implClass.getDeclaredConstructor().newInstance());
            } catch (Exception e) {
                log.error("load retrievalFilter fail:{}", implClass.getName(), e);;
            }
        }
        return bean;
    }

    /**
     * 顺序过滤
     */
    public static void doFilter(Set<RsInfo> rsInfoSet, Map<String, String> abMap) {
        if (CollUtil.isEmpty(filters) || CollUtil.isEmpty(rsInfoSet)) return;
        // 过滤未配置label的召回源
        Set<RsInfo> rsInfos = rsInfoSet.stream().filter(rs -> Objects.isNull(rs.getLabel())).collect(Collectors.toSet());
        AtomicBoolean suspend = new AtomicBoolean(false);
        filters.stream().sorted(Comparator.comparing(IRetrievalFilter::index))
                        .forEach(filter -> filter.sift(rsInfos, abMap, suspend));
    }


}
