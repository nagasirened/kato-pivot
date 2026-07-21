package com.kato.pro.langchain.config.refresh;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 启动期注册 KatoConfigProperties。
 *
 * 设计：
 *   - 不强制业务方 @Component 注入
 *   - 走 @EnableConfigurationProperties 模式（与现有 Properties 类一致）
 *   - 未来扩展：在 @ConditionalOnProperty(name = "kato.config.refresh.enabled", havingValue = "true")
 *     控制的 Bean 上挂这个 properties
 */
@Configuration
@EnableConfigurationProperties(KatoConfigProperties.class)
public class KatoConfigRefreshConfig {
}
