package com.kato.pro.register;

import cn.hutool.core.collection.CollUtil;
import com.kato.pro.constant.ConstantClass;
import com.kato.pro.constant.ServiceInfo;
import lombok.extern.slf4j.Slf4j;
import org.redisson.Redisson;
import org.redisson.api.RKeys;
import org.redisson.api.RList;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class RedissonRegisterService implements RegisterService {

    private RedissonClient redissonClient;

    public RedissonRegisterService(String serverAddress) {
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

    @Override
    public void register(ServiceInfo serviceInfo) {
        RList<Object> rList = redissonClient.getList(ConstantClass.REDISSON_REMOTE_SERVICE_PREFIX + serviceInfo.getServiceVersion());
        rList.add(serviceInfo);
    }

    @Override
    public void unregister(ServiceInfo serviceInfo) {
        RList<Object> rList = redissonClient.getList(ConstantClass.REDISSON_REMOTE_SERVICE_PREFIX + serviceInfo.getServiceVersion());
        rList.remove(serviceInfo);
    }

    @Override
    public void destroy(List<String> serviceVersionList) throws IOException {
        RKeys rKeys = redissonClient.getKeys();
        for (int i = 0; i < serviceVersionList.size(); i++) {
            rKeys.deleteAsync(ConstantClass.REDISSON_REMOTE_SERVICE_PREFIX + serviceVersionList.get(i));
        }
    }

}
