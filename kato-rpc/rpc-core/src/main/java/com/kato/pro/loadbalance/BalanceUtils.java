package com.kato.pro.loadbalance;

import cn.hutool.core.collection.CollUtil;

import java.util.List;

/**
 * @ClassName BalanceUtils
 * @Author Zeng Guangfu
 * @Description 负载均衡辅助器
 * @Date 2022/4/1 2:39 下午
 * @Version 1.0
 */
public class BalanceUtils {

    /**
     * 校验
     */
    public static <ServiceInfo> boolean valid(List<ServiceInfo> services) {
        if (CollUtil.isEmpty(services)) {
            return false;
        }
        return true;
    }

}
