package com.kato.pro.langchain.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 审计 JSON 文件输出配置（spec §6 M14）。
 *
 * 绑定 application.yml 的 kato.audit.file.* 段。
 *
 *   - enabled       : 是否启用 JSON 文件 appender（默认 false，本地开发避免意外产生大量日志）
 *   - path          : 输出文件路径（默认 logs/op-audit.json，相对工作目录）
 *   - maxLineLength : 单行 JSON 长度上限（超长截断 + "..." 后缀），避免一行写满磁盘 inode
 *
 * 设计取舍：
 *   - v1 不做文件轮转（建议生产挂 filebeat 外部轮转）
 *   - 写失败仅 log warn，不抛（审计不能影响主业务）
 */
@Data
@ConfigurationProperties(prefix = "kato.audit.file")
public class AuditJsonProperties {

    private boolean enabled = false;
    private String path = "logs/op-audit.json";
    private int maxLineLength = 8000;
}
