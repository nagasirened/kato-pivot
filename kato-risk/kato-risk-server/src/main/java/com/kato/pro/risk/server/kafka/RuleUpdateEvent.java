package com.kato.pro.risk.server.kafka;

import lombok.Data;

/**
 * Kafka 规则变更事件消息体。
 *
 * JSON 格式：
 * {
 *   "ruleId": 123,
 *   "version": 2,
 *   "eventType": "UPDATE"   // CREATE / UPDATE / DELETE / ENABLE / DISABLE
 * }
 */
@Data
public class RuleUpdateEvent {
    private Long ruleId;
    private Integer version;
    private String eventType;
}
