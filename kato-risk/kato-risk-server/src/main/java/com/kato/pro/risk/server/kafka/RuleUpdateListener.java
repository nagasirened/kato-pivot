package com.kato.pro.risk.server.kafka;

import com.kato.pro.risk.server.service.RuleEngineService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

/**
 * Kafka 消费者：监听敏感词/规则变更事件。
 *
 * Topic: risk-rule-update
 * Group: risk-server
 *
 * 消息格式（JSON）:
 * {
 *   "ruleId": 123,
 *   "version": 2,
 *   "eventType": "UPDATE"   // CREATE / UPDATE / DELETE / ENABLE / DISABLE
 * }
 *
 * 收到事件后触发 RuleEngineService.reloadRule(ruleId, version) 热更新规则。
 *
 * Phase 1: 仅监听，不处理 eventType 差异（均执行 reload）。
 * 业务扩展时按 eventType 分支处理。
 */
@Slf4j
@Component
@EnableBinding(RuleChannel.class)
public class RuleUpdateListener {

    @Autowired
    private RuleEngineService ruleEngineService;

    @StreamListener(RuleChannel.INPUT)
    public void onRuleUpdate(@Payload RuleUpdateEvent event) {
        if (event == null || event.getRuleId() == null) {
            log.warn("[RuleUpdateListener] 收到空消息或 ruleId 为空，忽略");
            return;
        }
        log.info("[RuleUpdateListener] 收到规则变更事件, ruleId={}, version={}, eventType={}",
                event.getRuleId(), event.getVersion(), event.getEventType());
        try {
            ruleEngineService.reloadRule(event.getRuleId(), event.getVersion());
            log.info("[RuleUpdateListener] 热更新完成, ruleId={}", event.getRuleId());
        } catch (Exception e) {
            log.error("[RuleUpdateListener] 热更新失败, ruleId={}", event.getRuleId(), e);
        }
    }
}
