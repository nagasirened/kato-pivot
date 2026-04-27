package com.kato.pro.langchain.demo.chat.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kato.pro.langchain.demo.chat.store.RedisChatMemoryStore;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

@Configuration
public class MemoryConfiguration {

    @Bean
    public ChatMemoryStore redisChatMemoryStore(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        return new RedisChatMemoryStore(redisTemplate, objectMapper);
    }

    /**
     * maxMessages(10) 代表最多存储 10 条消息，这相当于 5 次完整的问答轮次。
     */
    @Bean
    public ChatMemoryProvider separateChatMemoryProvider(@Autowired ChatMemoryStore redisChatMemoryStore) {
        return memoryId -> MessageWindowChatMemory.builder()
                .id(memoryId)
                .maxMessages(10)
                .chatMemoryStore(redisChatMemoryStore)
                .build();
    }

}
