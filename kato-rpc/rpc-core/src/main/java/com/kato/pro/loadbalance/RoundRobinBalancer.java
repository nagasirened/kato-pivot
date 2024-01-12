package com.kato.pro.loadbalance;

import com.kato.pro.constant.ServiceInfo;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @ClassName RoundRobinBalancer
 * @Author Zeng Guangfu
 * @Description 轮询
 * @Date 2022/4/1 2:40 下午
 * @Version 1.0
 */
public class RoundRobinBalancer implements LoadBalancer {

    AtomicInteger counter = new AtomicInteger(0);

    @Override
    public ServiceInfo chooseOne(List<ServiceInfo> services) {
        int index = increaseAndGet(services.size());
        return services.get(index);
    }

    private int increaseAndGet(int size) {
        while (true) {
            int i = counter.get();
            int j = (i + 1) % size;
            if (counter.compareAndSet(i, j)) {
                return j;
            }
        }
    }

}
