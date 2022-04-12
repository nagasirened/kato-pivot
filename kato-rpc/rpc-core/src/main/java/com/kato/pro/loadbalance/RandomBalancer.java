package com.kato.pro.loadbalance;

import cn.hutool.core.util.RandomUtil;
import com.kato.pro.constant.ServiceInfo;

import java.util.List;

/**
 * @ClassName RandomBalancer
 * @Author Zeng Guangfu
 * @Description 随机获取
 * @Date 2022/4/1 3:02 下午
 * @Version 1.0
 */
public class RandomBalancer implements LoadBalancer {

    @Override
    public ServiceInfo chooseOne(List<ServiceInfo> services) {
        return RandomUtil.randomEle(services);
    }

}
