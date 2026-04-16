package com.kato.pro.langchain.chat.store;

import java.time.Duration;

/**
 * Enum defining different chat memory types with their respective Redis key
 * prefixes and TTLs.
 */
public enum RedisChatMemoryEnum {

    /**
     * Default configuration for chat memory.
     */
    DEFAULT("chat-memory:", Duration.ofMinutes(30)),

    /**
     * Example: Assistant specific memory with longer TTL.
     */
    ASSISTANT("assistant:chat-memory:", Duration.ofDays(7)),

    /**
     * Example: Pivot specific memory with different TTL.
     */
    PIVOT("pivot:chat-memory:", Duration.ofDays(1));

    private final String prefix;
    private final Duration ttl;

    RedisChatMemoryEnum(String prefix, Duration ttl) {
        this.prefix = prefix;
        this.ttl = ttl;
    }

    public String getPrefix() {
        return prefix;
    }

    public Duration getTtl() {
        return ttl;
    }

    /**
     * Resolves the {@link RedisChatMemoryEnum} based on the memoryId.
     * If memoryId is a String and starts with an enum name followed by a colon
     * (e.g., "ASSISTANT:123"),
     * it returns the corresponding enum. Otherwise, it returns {@link #DEFAULT}.
     */
    public static RedisChatMemoryEnum resolve(Object memoryId) {
        if (memoryId instanceof String strId) {
            int colonIndex = strId.indexOf(":");
            if (colonIndex > 0) {
                String typeStr = strId.substring(0, colonIndex);
                try {
                    return RedisChatMemoryEnum.valueOf(typeStr.toUpperCase());
                } catch (IllegalArgumentException e) {
                    // Fall back to default if type is not recognized
                }
            }
        }
        return DEFAULT;
    }

    /**
     * Extracts the actual identifier from a composite memoryId (e.g.,
     * "ASSISTANT:123" -> "123").
     */
    public static String getIdentifier(Object memoryId) {
        if (memoryId instanceof String strId) {
            int colonIndex = strId.indexOf(":");
            if (colonIndex > 0) {
                return strId.substring(colonIndex + 1);
            }
            return strId;
        }
        return String.valueOf(memoryId);
    }
}
