package com.kato.pro.langchain.config.refresh;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Kato 平台级配置（kato.*）。
 *
 * 当前覆盖：
 *   - kato.config.refresh.enabled       : 是否启用 ConfigRefresher 监听（默认 true）
 *   - kato.config.refresh.watched-keys  : 业务方关心的 key 列表（健康日志用）
 *
 * 其他 kato.* 子模块（kato.audit.* / kato.metrics.*）由各自的 @ConfigurationProperties 承载，
 * 不在此处聚合。
 */
@Data
@ConfigurationProperties(prefix = "kato.config")
public class KatoConfigProperties {

    private Refresh refresh = new Refresh();

    @Data
    public static class Refresh {
        private boolean enabled = true;
        private Map<String, String> watchedKeys = new LinkedHashMap<>();
    }
}
