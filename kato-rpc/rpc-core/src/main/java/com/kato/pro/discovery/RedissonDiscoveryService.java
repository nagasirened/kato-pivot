package com.kato.pro.discovery;

import cn.hutool.core.collection.CollUtil;
import com.kato.pro.constant.ConstantClass;
import com.kato.pro.constant.ServiceInfo;
import com.kato.pro.loadbalance.LoadBalancer;
import lombok.extern.slf4j.Slf4j;
import org.redisson.Redisson;
import org.redisson.api.RList;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;

import javax.annotation.PreDestroy;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class RedissonDiscoveryService implements DiscoveryService{

    private RedissonClient redissonClient;

    private final LoadBalancer loadBalancer;

    public RedissonDiscoveryService(String serverAddress, LoadBalancer loadBalancer) {
        this.loadBalancer = loadBalancer;
        try {
            AtomicInteger count = new AtomicInteger(0);
            while (count.addAndGet(1) <= 3) {
                try {
                    Config config = new Config();
                    config.useSingleServer().setAddress(serverAddress);
                    this.redissonClient = Redisson.create(config);
                    break;
                } catch (Exception ignored) { }
            }
            if (Objects.isNull(redissonClient)) {
                throw new RuntimeException("redisson serviceDiscovery init fail!");
            }
        } catch (Exception e) {
            log.error("Redisson ServiceDiscovery error, message: {}", e.getMessage(), e);
        }
    }

    /**
     * 没弄明白RRemoteService，不好使，直接使用 ListMultimap
     */
    @Override
    public ServiceInfo discover(String serviceName) {
        RList<ServiceInfo> rList = redissonClient.getList(ConstantClass.REDISSON_REMOTE_SERVICE_PREFIX + serviceName);
        List<ServiceInfo> serviceInfoList = rList.readAll();
        if (CollUtil.isEmpty(serviceInfoList)) {
            return null;
        }
        return loadBalancer.chooseOne(serviceInfoList);
    }

    @PreDestroy
    public void destroy() {
        this.redissonClient.shutdown();
    }
}
