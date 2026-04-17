package com.kato.pro.sensitive.kafka;

import com.kato.pro.sensitive.service.impl.SensitiveWordLoadServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * 敏感词Kafka消费者
 * 接收敏感词更新消息，触发热更新
 */
@Slf4j
@Component
public class SensitiveWordKafkaConsumer {

    private final SensitiveWordLoadServiceImpl loadService;

    public SensitiveWordKafkaConsumer(SensitiveWordLoadServiceImpl loadService) {
        this.loadService = loadService;
    }

    @KafkaListener(topics = "${kafka.topic.sensitive-update:sensitive-word-update}", groupId = "${spring.application.name}")
    public void onMessage(String message) {
        log.info("收到敏感词更新消息: {}", message);
        try {
            loadService.hotReloadSensitiveWords();
            log.info("敏感词热更新完成");
        } catch (Exception e) {
            log.error("敏感词热更新失败", e);
        }
    }
}
