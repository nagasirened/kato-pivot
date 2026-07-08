package com.kato.pro.risk.client.dto;

import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * 风控检查请求上下文。
 *
 * Phase 1 骨架，字段按注册/登录场景设计，订单场景补充 orderAmount/shippingAddress。
 */
@Data
public class RiskRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 环节：REGISTER / LOGIN / MARKETING / ORDER / PAYMENT / REFUND */
    private String scene;

    /** 用户唯一标识 */
    private String userId;

    /** 设备指纹 */
    private String deviceId;

    /** 请求来源 IP */
    private String ip;

    /** 订单金额（ORDER / PAYMENT 场景必填） */
    private BigDecimal orderAmount;

    /** 收货地址（ORDER 场景填写） */
    private String shippingAddress;

    /** 银行卡号（PAYMENT 场景填写） */
    private String cardNo;

    /** 请求唯一 ID（用于幂等） */
    private String requestId;

    /** 扩展字段（gRPC 传输用） */
    private Map<String, String> extData = new HashMap<>();
}