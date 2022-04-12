package com.kato.pro.discovery;

import cn.hutool.core.collection.CollUtil;
import com.kato.pro.constant.ServiceInfo;
import com.kato.pro.loadbalance.LoadBalancer;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.curator.x.discovery.ServiceDiscovery;
import org.apache.curator.x.discovery.ServiceDiscoveryBuilder;
import org.apache.curator.x.discovery.ServiceInstance;
import org.apache.curator.x.discovery.details.JsonInstanceSerializer;

import java.util.Collection;
import java.util.stream.Collectors;

/**
 * 基于Zookeeper的服务发现器
 */
@Slf4j
public class CuratorDiscoveryService implements DiscoveryService {

    public static final String BASE_PATH = "/kato_discover";

    /** curator 服务注册器 */
    private ServiceDiscovery<ServiceInfo> serviceDiscovery;
    /** 负载均衡器 */
    private final LoadBalancer loadBalancer;

    public CuratorDiscoveryService(String serverAddress, LoadBalancer loadBalancer) {
        this.loadBalancer = loadBalancer;
        try {
            // 创建client连接；  重试3次，每次等待1000ms
            ExponentialBackoffRetry retryPolicy = new ExponentialBackoffRetry(1000, 3);
            CuratorFramework client = CuratorFrameworkFactory.newClient(serverAddress, retryPolicy);
            client.start();

            // 启动服务注册器
            this.serviceDiscovery = ServiceDiscoveryBuilder.builder(ServiceInfo.class)
                    .client(client)
                    .basePath(BASE_PATH)
                    .serializer(new JsonInstanceSerializer<>(ServiceInfo.class))
                    .build();
            serviceDiscovery.start();
        } catch (Exception e) {
            log.error("Curator ServiceDiscovery error, message: {}", e.getMessage(), e);
        }
    }

    /**
     * 根据serviceName获取对应的服务列表，选择其中一个作为响应的服务
     */
    @Override
    public ServiceInfo discover(String serviceName) throws Exception {
        Collection<ServiceInstance<ServiceInfo>> serviceInstances = serviceDiscovery.queryForInstances(serviceName);
        if (CollUtil.isEmpty(serviceInstances)) {
            return null;
        }
        return loadBalancer.chooseOne(serviceInstances.stream().map(ServiceInstance::getPayload).collect(Collectors.toList()));
    }
}
