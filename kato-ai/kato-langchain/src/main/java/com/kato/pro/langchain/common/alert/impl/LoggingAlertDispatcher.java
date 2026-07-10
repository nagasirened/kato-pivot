package com.kato.pro.langchain.common.alert.impl;

import com.kato.pro.langchain.common.alert.AlertDispatcher;
import com.kato.pro.langchain.common.alert.AlertEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 日志告警分发器（兜底）：所有告警写到 error 日志。
 */
@Slf4j
@Component
public class LoggingAlertDispatcher implements AlertDispatcher {

    @Override
    public String name() { return "logging"; }

    @Override
    public void dispatch(AlertEvent event) {
        log.error("ALERT [{}] rule={} message={}",
                event.severity(), event.ruleName(), event.message());
    }
}
