package com.kato.pro.loadbalance;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.RandomUtil;
import com.kato.pro.constant.ServiceInfo;

import java.util.List;
import java.util.Objects;
import java.util.Random;

/**
 * @ClassName RandomBalancer
 * @Author Zeng Guangfu
 * @Description 随机获取
 * @Date 2022/4/1 3:02 下午
 * @Version 1.0
 */
public class RandomBalancer implements LoadBalancer {

    public final Random random = new Random();


    @Override
    public ServiceInfo chooseOne(List<ServiceInfo> services) {
        if (CollUtil.isEmpty(services)) {
            return null;
        }

        if (services.size() == 1) {
            return services.get(0);
        }

        return services.get(random.nextInt(services.size()));
    }

}
