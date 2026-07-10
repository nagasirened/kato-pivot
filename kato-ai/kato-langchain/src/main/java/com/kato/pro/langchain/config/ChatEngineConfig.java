package com.kato.pro.langchain.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(ChatEngineProperties.class)
public class ChatEngineConfig {
}
