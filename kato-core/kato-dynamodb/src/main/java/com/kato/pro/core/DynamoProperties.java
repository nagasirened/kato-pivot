package com.kato.pro.core;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "aws.access")
public class DynamoProperties {

    private String key;

    private String secret;

    private String region;

    private Integer maxTotal = 20;

    private Integer minIdle = 5;

    private Integer maxIdle = 10;

    private Integer maxWaitMillis = 5000;

}
