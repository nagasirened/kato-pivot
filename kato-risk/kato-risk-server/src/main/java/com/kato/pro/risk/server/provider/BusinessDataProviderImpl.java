package com.kato.pro.risk.server.provider;

import com.kato.pro.risk.client.dto.BusinessDataProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * 业务数据查询接口空实现（Phase 1）。
 *
 * 业务方需自行实现该接口并注册为 Spring Bean，覆盖此空实现。
 * 风控 Client 在执行规则时自动调用业务方提供的真实数据。
 *
 * 当前为空实现仅用于 Phase 1 骨架验证。
 * 实际业务数据由业务系统通过 @Primary 或条件装配注入。
 */
@Slf4j
@Component
@Primary
public class BusinessDataProviderImpl implements BusinessDataProvider {

    @Override
    public List<Object> getRecentOrders(String userId, int days) {
        log.warn("[BusinessDataProvider] 空实现被调用，请业务方注入真实实现 bean. userId={}, days={}", userId, days);
        return Collections.emptyList();
    }

    @Override
    public List<Object> getUserAddresses(String userId) {
        log.warn("[BusinessDataProvider] 空实现被调用，请业务方注入真实实现 bean. userId={}", userId);
        return Collections.emptyList();
    }

    @Override
    public List<Object> getRefundRecords(String userId, int days) {
        log.warn("[BusinessDataProvider] 空实现被调用，请业务方注入真实实现 bean. userId={}, days={}", userId, days);
        return Collections.emptyList();
    }

    @Override
    public List<Object> getLoginHistory(String userId, int days) {
        log.warn("[BusinessDataProvider] 空实现被调用，请业务方注入真实实现 bean. userId={}, days={}", userId, days);
        return Collections.emptyList();
    }
}
