package com.kato.pro.config;

import com.kato.pro.props.RedissonProperties;
import com.kato.pro.props.RedissonType;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.Resource;

@Configuration
@EnableConfigurationProperties(RedissonProperties.class)
@ConditionalOnProperty(value = RedissonProperties.PREFIX + ".enable", havingValue = "true", matchIfMissing = true)
public class RedissonConfiguration {

    @Resource
    private RedissonProperties redissonProperties;

    @Bean
    @ConditionalOnMissingBean(name = "redissonClient")
    public RedissonClient redissonClient() {
        RedissonType redissonType = redissonProperties.getType();
        Config config = redissonType.pack(redissonProperties);
        return Redisson.create(config);
    }

}
