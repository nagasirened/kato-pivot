package com.kato.pro.langchain.demo.chat.service;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import dev.langchain4j.service.spring.AiService;
import dev.langchain4j.service.spring.AiServiceWiringMode;

// 加入到了AssistantConfiguration中，拓展了额外的transformer属性
@AiService(wiringMode = AiServiceWiringMode.EXPLICIT, chatModel = "", chatMemoryProvider = "separateChatMemoryProvider")
public interface SeparateAssistant {

    @SystemMessage("{{dynamicSystemMessage}}")
    String chat(@MemoryId String memoryId,
            @V("dynamicSystemMessage") String dynamicSystemMessage,
            @UserMessage String userMessage);

    @SystemMessage(fromResource = "base_system.txt")
    String chatFromResource(@MemoryId String memoryId,
            @V("dynamicSystemMessage") String dynamicSystemMessage,
            @UserMessage String userMessage);

    String chat(String userMessage);

    /**
     * AI Service支持的返回类型
     * * String
     * * 基本类型 boolean/byte/short/int/long/float/double
     * * 对象类型 Boolean/Byte/Short/Integer/Long/Float/Double
     * * 时间类型 Date/LocalDate/LocalTime/LocalDateTime
     * * 集合类型 List<String>/Set<String>
     * * 枚举类型 Enum
     * * 自定义POJO
     * * 自定义Result<T>
     * * 大模型回复消息 AiMessage
     */

}
