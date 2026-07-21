package com.kato.pro.langchain.config.refresh;

import com.kato.pro.langchain.config.source.ConfigSnapshot;
import org.springframework.context.ApplicationEvent;

/**
 * Kato 自定义配置变更事件。
 *
 * 触发场景：
 *   - 业务方手动调 ConfigRefresher.refreshNow() 主动 reload
 *   - M17+ NacosConfigSource 收到 server 推送时
 *   - 单元测试模拟 Environment 变更时
 *
 * 不依赖 spring-cloud-context（沙箱 m2 不可用）。
 */
public class KatoConfigChangeEvent extends ApplicationEvent {

    private final ConfigSnapshot snapshot;
    private final String reason;

    public KatoConfigChangeEvent(Object source, ConfigSnapshot snapshot, String reason) {
        super(source);
        this.snapshot = snapshot;
        this.reason = reason;
    }

    public ConfigSnapshot getSnapshot() {
        return snapshot;
    }

    public String getReason() {
        return reason;
    }
}
