package com.kato.pro.langchain.config;

import com.kato.pro.langchain.domain.tool.DefaultToolInvoker;
import com.kato.pro.langchain.domain.tool.SimpleCircuitBreaker;
import com.kato.pro.langchain.domain.tool.ToolInvoker;
import com.kato.pro.langchain.domain.tool.ToolProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(ToolProperties.class)
public class ToolConfig {

    @Bean
    public SimpleCircuitBreaker simpleCircuitBreaker(ToolProperties props) {
        return new SimpleCircuitBreaker(props.getCbFailureThreshold(), props.getCbResetMs());
    }

    @Bean
    public ToolInvoker toolInvoker(SimpleCircuitBreaker cb) {
        return new DefaultToolInvoker(cb);
    }
}
