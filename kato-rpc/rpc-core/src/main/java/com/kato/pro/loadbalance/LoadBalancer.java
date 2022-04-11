package com.kato.pro.loadbalance;

import cn.hutool.core.collection.CollUtil;

import java.util.List;


/**
 * @ClassName LoadBalancer
 * @Author Zeng Guangfu
 * @Description 负载均衡器 - 接口
 * @Date 2022/4/1 2:36 下午
 * @Version 1.0
 */
public interface LoadBalancer<ServiceInfo> {

    ServiceInfo chooseOne(List<ServiceInfo> services);

    default ServiceInfo choose(List<ServiceInfo> services) {
        if (!requireNotEmpty(services)) {
            return null;
        }
        return chooseOne(services);
    }

    default <T> boolean requireNotEmpty(List<T> services) {
        return !CollUtil.isEmpty(services);
    }
}
