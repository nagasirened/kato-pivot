package com.kato.pro.langchain.config.source;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * Nacos 配置源配置（kato.config.nacos.*）。
 *
 *   kato.config.nacos.enabled         是否启用（默认 false → 走 LocalConfigSource）
 *   kato.config.nacos.server-addr     Nacos server 地址
 *   kato.config.nacos.namespace       namespace ID（多环境隔离用）
 *   kato.config.nacos.username/password  鉴权（生产开）
 *   kato.config.nacos.group           dataId 分组（默认 DEFAULT_GROUP）
 *   kato.config.nacos.data-ids        监听的 dataId 列表
 *   kato.config.nacos.timeout-ms      拉取超时（毫秒，默认 10s）
 */
@Data
@ConfigurationProperties(prefix = "kato.config.nacos")
public class NacosConfigProperties {

    private boolean enabled = false;
    private String serverAddr = "127.0.0.1:8848";
    private String namespace = "";
    private String username = "";
    private String password = "";
    private String group = "DEFAULT_GROUP";
    private List<String> dataIds = new ArrayList<>();
    private long timeoutMs = 10_000L;
}
