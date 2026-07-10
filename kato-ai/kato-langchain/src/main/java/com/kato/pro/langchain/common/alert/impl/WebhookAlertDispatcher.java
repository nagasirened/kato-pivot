package com.kato.pro.langchain.common.alert.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kato.pro.langchain.common.alert.AlertDispatcher;
import com.kato.pro.langchain.common.alert.AlertEvent;
import com.kato.pro.langchain.common.alert.AlertProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;

/**
 * Webhook 告警分发器：HTTP POST JSON 到 alert.webhook-url。
 */
@Slf4j
@Component
public class WebhookAlertDispatcher implements AlertDispatcher {

    private final AlertProperties properties;
    private final ObjectMapper objectMapper;
    private final WebClient webClient;

    public WebhookAlertDispatcher(AlertProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.webClient = WebClient.builder().build();
    }

    @Override
    public String name() { return "webhook"; }

    @Override
    public void dispatch(AlertEvent event) {
        String url = properties.getWebhookUrl();
        if (url == null || url.isBlank()) {
            log.debug("WebhookAlertDispatcher skip: no webhookUrl configured");
            return;
        }
        try {
            String body = objectMapper.writeValueAsString(event);
            webClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(body)
                    .retrieve()
                    .toBodilessEntity()
                    .block(Duration.ofSeconds(5));
            log.info("Alert dispatched to webhook: rule={} severity={}", event.ruleName(), event.severity());
        } catch (Exception e) {
            log.warn("WebhookAlertDispatcher failed: rule={} error={}", event.ruleName(), e.getMessage());
        }
    }
}
