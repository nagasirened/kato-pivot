package com.kato.pro.langchain.common.alert;

import com.kato.pro.langchain.common.alert.impl.ThresholdRule;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Alert 配置 + 内置规则注册（spec §6 M11）。
 *
 * 内置 3 个规则：
 *   - safety-reject-high    : safety.reject.total >= 50 → WARN
 *   - tool-fail-high        : tool.fail.total >= 10 → WARN
 *   - chat-error-critical   : chat.error.total >= 20 → CRITICAL
 */
@Configuration
@EnableConfigurationProperties(AlertProperties.class)
public class AlertConfig {

    @Bean
    public AlertRule safetyRejectHigh() {
        return new ThresholdRule(
                "safety-reject-high",
                "safety.reject.total",
                50.0,
                AlertSeverity.WARN,
                "安全拒绝次数过多，疑似有攻击");
    }

    @Bean
    public AlertRule toolFailHigh() {
        return new ThresholdRule(
                "tool-fail-high",
                "tool.fail.total",
                10.0,
                AlertSeverity.WARN,
                "工具调用失败次数过多");
    }

    @Bean
    public AlertRule chatErrorCritical() {
        return new ThresholdRule(
                "chat-error-critical",
                "chat.error.total",
                20.0,
                AlertSeverity.CRITICAL,
                "对话错误率过高，需要立即排查");
    }
}
