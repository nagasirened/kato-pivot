package com.kato.pro.loadbalance;

import java.util.List;

import static com.kato.pro.loadbalance.BalanceUtils.valid;

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
        if (!valid(services)) {
            return null;
        }
        return chooseOne(services);
    }
}
