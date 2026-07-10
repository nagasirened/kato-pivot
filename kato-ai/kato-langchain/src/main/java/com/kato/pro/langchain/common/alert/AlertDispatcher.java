package com.kato.pro.langchain.common.alert;

/**
 * 告警分发器 SPI（M11）。
 *
 * 实现：WebhookAlertDispatcher、LoggingAlertDispatcher。
 * 失败时不抛错（log 即可），不影响主业务。
 */
public interface AlertDispatcher {

    /** 分发器名 */
    String name();

    /** 分发告警事件 */
    void dispatch(AlertEvent event);
}
