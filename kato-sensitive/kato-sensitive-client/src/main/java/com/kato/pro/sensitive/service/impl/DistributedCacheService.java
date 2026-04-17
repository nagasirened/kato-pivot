package com.kato.pro.sensitive.service.impl;

import com.kato.pro.sensitive.entity.SensitiveWord;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 分布式缓存服务
 * 实现多实例环境下的敏感词同步
 */
@Slf4j
@Service
public class DistributedCacheService {

    private static final String KEY_VERSION = "sensitive:version";
    private static final String KEY_WORDS_PREFIX = "sensitive:words:";
    private static final String KEY_DFA = "sensitive:dfa";

    @Resource
    private StringRedisTemplate redisTemplate;

    /**
     * 设置当前版本号
     */
    public void setVersion(long version) {
        redisTemplate.opsForValue().set(KEY_VERSION, String.valueOf(version));
    }

    /**
     * 获取当前版本号
     */
    public long getVersion() {
        String version = redisTemplate.opsForValue().get(KEY_VERSION);
        return version != null ? Long.parseLong(version) : 0;
    }

    /**
     * 存储敏感词（按等级）
     */
    public void setWords(String level, List<SensitiveWord> words) {
        String key = KEY_WORDS_PREFIX + level;
        StringBuilder sb = new StringBuilder();
        for (SensitiveWord word : words) {
            sb.append(word.getWord()).append(",");
        }
        redisTemplate.opsForValue().set(key, sb.toString());
    }

    /**
     * 获取敏感词（按等级）
     */
    public List<String> getWords(String level) {
        String key = KEY_WORDS_PREFIX + level;
        String words = redisTemplate.opsForValue().get(key);
        if (words == null || words.isEmpty()) {
            return List.of();
        }
        return List.of(words.split(","));
    }

    /**
     * 存储DFA状态机序列化
     */
    public void setDfaState(String dfaState) {
        redisTemplate.opsForValue().set(KEY_DFA, dfaState);
    }

    /**
     * 获取DFA状态机序列化
     */
    public String getDfaState() {
        return redisTemplate.opsForValue().get(KEY_DFA);
    }

    /**
     * 使缓存失效（版本号变更时调用）
     */
    public void invalidate() {
        redisTemplate.delete(KEY_VERSION);
        redisTemplate.delete(KEY_WORDS_PREFIX + "URGENT");
        redisTemplate.delete(KEY_WORDS_PREFIX + "MEDIUM");
        redisTemplate.delete(KEY_WORDS_PREFIX + "NORMAL");
        redisTemplate.delete(KEY_DFA);
        log.info("分布式缓存已失效");
    }

    /**
     * 检查是否是最新版本
     */
    public boolean isLatestVersion(long localVersion) {
        long remoteVersion = getVersion();
        return localVersion >= remoteVersion;
    }

    /**
     * 缓存敏感词（设置过期时间）
     */
    public void cacheWithExpire(String key, String value, long timeout, TimeUnit unit) {
        redisTemplate.opsForValue().set(key, value, timeout, unit);
    }
}
