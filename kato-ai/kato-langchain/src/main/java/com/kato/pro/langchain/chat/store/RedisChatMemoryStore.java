package com.kato.pro.langchain.chat.store;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.Collections;
import java.util.List;

/**
 * Implementation of {@link ChatMemoryStore} that uses Redis for storage with dynamic prefix and expiration.
 * The configuration is resolved dynamically via {@link RedisChatMemoryEnum} based on the memoryId.
 */
public class RedisChatMemoryStore implements ChatMemoryStore {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public RedisChatMemoryStore(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public List<ChatMessage> getMessages(Object memoryId) {
        RedisChatMemoryEnum config = RedisChatMemoryEnum.resolve(memoryId);
        String key = key(memoryId, config);
        String json = redisTemplate.opsForValue().get(key);
        if (json == null || json.isEmpty()) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<ChatMessage>>() {});
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to deserialize chat messages for memoryId: " + memoryId, e);
        }
    }

    @Override
    public void updateMessages(Object memoryId, List<ChatMessage> messages) {
        RedisChatMemoryEnum config = RedisChatMemoryEnum.resolve(memoryId);
        String key = key(memoryId, config);
        
        if (messages == null || messages.isEmpty()) {
            redisTemplate.delete(key);
            return;
        }
        try {
            String json = objectMapper.writeValueAsString(messages);
            if (config.getTtl() != null) {
                redisTemplate.opsForValue().set(key, json, config.getTtl());
            } else {
                redisTemplate.opsForValue().set(key, json);
            }
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize chat messages for memoryId: " + memoryId, e);
        }
    }

    @Override
    public void deleteMessages(Object memoryId) {
        RedisChatMemoryEnum config = RedisChatMemoryEnum.resolve(memoryId);
        redisTemplate.delete(key(memoryId, config));
    }

    private String key(Object memoryId, RedisChatMemoryEnum config) {
        return config.getPrefix() + RedisChatMemoryEnum.getIdentifier(memoryId);
    }
}
