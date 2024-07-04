package com.kato.pro.redisson.config;

import com.kato.pro.redisson.lock.realize.RedissonDistributeLock;
import com.kato.pro.redisson.props.RedissonProperties;
import com.kato.pro.redisson.props.RedissonType;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.Order;

import javax.annotation.Resource;

@EnableConfigurationProperties(value = {RedissonProperties.class})
@ConditionalOnProperty(value = RedissonProperties.PREFIX + ".enable", havingValue = "true", matchIfMissing = true)
public class RedissonConfiguration {

    @Resource
    private RedissonProperties redissonProperties;

    /**
     * 根据不同的redisson类型，加载对应的客户端对象8
     */
    @Bean
    @ConditionalOnMissingBean(name = "redissonClient")
    public RedissonClient redissonClient() {
        RedissonType redissonType = redissonProperties.getType();
        Config config = redissonType.pack(redissonProperties);
        return Redisson.create(config);
    }

    @Bean
    @ConditionalOnClass(RedissonClient.class)
    @ConditionalOnProperty(prefix = "kato.lock", name = "type", havingValue = "REDISSON", matchIfMissing = true)
    public RedissonDistributeLock redissonDistributeLock() {
        return new RedissonDistributeLock(redissonClient());
    }

}
