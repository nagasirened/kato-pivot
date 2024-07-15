package com.kato.pro.rec.service.core;

import com.kato.pro.redis.RedisService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
public class CacheService {

    @Resource
    private RedisService redisService;

    public String get(String key) {
        return (String) redisService.get(key);
    }

    public Object incr(String redisKey) {
        return incr(redisKey, 1L);
    }

    public Object incr(String redisKey, long num) {
        return redisService.incr(redisKey, num);
    }
}
