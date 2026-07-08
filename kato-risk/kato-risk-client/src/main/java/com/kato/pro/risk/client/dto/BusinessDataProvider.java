package com.kato.pro.risk.client.dto;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * 业务数据查询接口。
 *
 * 定义在 kato-risk-client，业务方实现该接口并注册为 Spring Bean。
 * 风控 Client 在执行规则时自动调用，业务方提供真实数据。
 *
 * Phase 1 骨架：仅定义方法签名，不实现。
 * 业务方按需实现，接口方法按设计文档覆盖：
 * - REGISTER: recent-orders, addresses
 * - LOGIN: login-history
 * - ORDER: recent-orders, addresses
 * - PAYMENT: recent-orders, addresses, refund-records
 * - REFUND: refund-records
 */
public interface BusinessDataProvider {

    @GetMapping("/internal/user/{userId}/recent-orders")
    List<Object> getRecentOrders(@PathVariable("userId") String userId,
                                 @RequestParam("days") int days);

    @GetMapping("/internal/user/{userId}/addresses")
    List<Object> getUserAddresses(@PathVariable("userId") String userId);

    @GetMapping("/internal/user/{userId}/refund-records")
    List<Object> getRefundRecords(@PathVariable("userId") String userId,
                                  @RequestParam("days") int days);

    @GetMapping("/internal/user/{userId}/login-history")
    List<Object> getLoginHistory(@PathVariable("userId") String userId,
                                @RequestParam("days") int days);
}
