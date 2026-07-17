package com.kato.pro.langchain.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 审计 JSON 文件输出配置注册（spec §6 M14）。
 *
 * 注册 AuditJsonProperties 让 @ConfigurationProperties(prefix = "kato.audit.file") 生效。
 */
@Configuration
@EnableConfigurationProperties(AuditJsonProperties.class)
public class AuditJsonConfig {
}
