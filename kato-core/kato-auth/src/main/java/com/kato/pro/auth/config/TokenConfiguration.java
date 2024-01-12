package com.kato.pro.auth.config;

import com.kato.pro.auth.aspect.PreAuthAspect;
import com.kato.pro.redis.RedisService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;


@ComponentScan(value = "com.kato.pro.auth")
@ConditionalOnProperty(value = "kato.auth.enable", havingValue = "true", matchIfMissing = true)
public class TokenConfiguration {

    @Bean
    public PreAuthAspect preAuthAspect(RedisService redisService) {
        return new PreAuthAspect(redisService);
    }

}
