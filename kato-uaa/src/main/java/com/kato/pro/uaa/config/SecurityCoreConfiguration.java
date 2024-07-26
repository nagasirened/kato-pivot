package com.kato.pro.uaa.config;

import com.kato.pro.uaa.properties.SecurityProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({SecurityProperties.class})
public class SecurityCoreConfiguration {


}
