package com.kato.pro.risk.server.kafka;

import org.springframework.cloud.stream.annotation.Input;
import org.springframework.messaging.SubscribableChannel;

/**
 * Spring Cloud Stream Channel 定义。
 *
 * 绑定 Kafka Topic: risk-rule-update
 */
public interface RuleChannel {

    String INPUT = "rule-update-input";

    @Input(INPUT)
    SubscribableChannel input();
}
