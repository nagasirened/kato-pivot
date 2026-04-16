package com.kato.pro.langchain.demo.assistant;


import com.kato.pro.langchain.demo.entity.IntentionOutput;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import dev.langchain4j.service.spring.AiService;
import dev.langchain4j.service.spring.AiServiceWiringMode;

/**
 * 同一个代理对象类，不同的系统提示词会覆盖之前的设置
 */
@AiService(
        wiringMode = AiServiceWiringMode.EXPLICIT,
        chatModel = "qwenChatModel",
        streamingChatModel = "qwenStreamingChatModel",
        chatMemoryProvider = "chatMemoryProvider",
        tools = "testTools"
)
@SystemMessage(fromResource = "/intention.txt")
public interface AiAssistant {
    
    @UserMessage("当前sessionId:{{sessionId}}，用户当前消息:{{message}}")
    IntentionOutput intention(@V("sessionId") String sessionId, @V("message")String message);

}
