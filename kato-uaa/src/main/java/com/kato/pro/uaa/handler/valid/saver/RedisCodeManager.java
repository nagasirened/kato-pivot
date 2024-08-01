package com.kato.pro.uaa.handler.valid.saver;

import com.kato.pro.redis.RedisService;
import com.kato.pro.uaa.entity.code.ValidateCode;
import org.springframework.web.context.request.ServletWebRequest;

import javax.annotation.Resource;
import java.util.Optional;
import java.util.concurrent.TimeUnit;


public class RedisCodeManager implements ICodeManager {

    @Resource
    private RedisService redisService;

    @Override
    public void save(ServletWebRequest webRequest, ValidateCode validateCode) {
        String redisKey = wrapSaveKey(validateCode.getValidPrefix());
        redisService.setExpire(redisKey, validateCode.getCode(), 300, TimeUnit.SECONDS);
    }

    @Override
    public String getCode(String randomPrefix, ServletWebRequest webRequest) {
        String redisKey = wrapSaveKey(randomPrefix);
        return Optional.ofNullable(redisService.get(redisKey))
                .map(String::valueOf)
                .orElse("");
    }

}
