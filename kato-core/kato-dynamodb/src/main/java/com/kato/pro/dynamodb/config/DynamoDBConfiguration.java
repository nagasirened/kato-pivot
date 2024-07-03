package com.kato.pro.dynamodb.config;


import com.kato.pro.dynamodb.core.DynamoDBClientFactory;
import com.kato.pro.dynamodb.core.DynamoProperties;
import com.kato.pro.common.pool.ClientManager;
import com.kato.pro.common.pool.DynamicObjectPool;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import javax.annotation.Resource;

@Configuration
@EnableConfigurationProperties( {DynamoProperties.class} )
public class DynamoDBConfiguration {

    @Resource
    private DynamoProperties dynamoProperties;

    @Bean
    @ConditionalOnProperty(prefix = "aws.access", name = {"key", "secret", "region"})
    public DynamicObjectPool<ClientManager<DynamoDbClient>> dynamoPool() {
        DynamoDBClientFactory factory = new DynamoDBClientFactory(dynamoProperties);
        DynamicObjectPool<ClientManager<DynamoDbClient>> objectPool = new DynamicObjectPool<>(DynamoDbClient.class.getSimpleName(), factory);
        objectPool.setMaxTotal(dynamoProperties.getMaxTotal());
        objectPool.setMinIdle(dynamoProperties.getMinIdle());
        objectPool.setMaxIdle(dynamoProperties.getMaxIdle());
        objectPool.setMaxWaitMillis(dynamoProperties.getMaxWaitMillis());
        return objectPool;
    }
}
