package com.kato.pro.register;

import com.kato.pro.constant.ConstantClass;
import com.kato.pro.constant.ServiceInfo;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.curator.x.discovery.ServiceDiscovery;
import org.apache.curator.x.discovery.ServiceDiscoveryBuilder;
import org.apache.curator.x.discovery.ServiceInstance;
import org.apache.curator.x.discovery.details.JsonInstanceSerializer;

import java.io.IOException;
import java.util.List;

@Slf4j
public class CuratorRegisterService implements RegisterService {

    private ServiceDiscovery<ServiceInfo> serviceDiscovery;

    public CuratorRegisterService(String serverAddress) {
        try {
            CuratorFramework client = CuratorFrameworkFactory.newClient(serverAddress, new ExponentialBackoffRetry(1000, 3));
            client.start();
            this.serviceDiscovery = ServiceDiscoveryBuilder.builder(ServiceInfo.class)
                    .basePath(ConstantClass.CURATOR_BASE_PATH)
                    .serializer(new JsonInstanceSerializer<>(ServiceInfo.class))
                    .client(client)
                    .build();
            serviceDiscovery.start();
        } catch (Exception e) {
            log.error("CuratorRegisterService, Curator ServiceDiscovery error, message: {}", e.getMessage(), e);
        }
    }

    @Override
    public void register(ServiceInfo serviceInfo) {
        try {
            this.serviceDiscovery.registerService(packageServiceInstance(serviceInfo));
        } catch (Exception e) {
            log.error("curator register serviceInstance fail", e);
        }
    }

    @Override
    public void unregister(ServiceInfo serviceInfo) {
        try {
            this.serviceDiscovery.unregisterService(packageServiceInstance(serviceInfo));
        } catch (Exception e) {
            log.error("curator unregister serviceInstance fail", e);
        }
    }

    private ServiceInstance<ServiceInfo> packageServiceInstance(ServiceInfo serviceInfo) throws Exception {
        return ServiceInstance.<ServiceInfo>builder()
                .name(serviceInfo.getServiceVersion())
                .address(serviceInfo.getAddress())
                .port(serviceInfo.getPort())
                .payload(serviceInfo)
                .build();
    }

    @Override
    public void destroy(List<String> serviceVersionList) throws IOException {
        this.serviceDiscovery.close();
    }
}
