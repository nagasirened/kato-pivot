package com.kato.pro.langchain.demo.assistant;

import com.kato.pro.langchain.demo.entity.LostRegisterOutput;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import dev.langchain4j.service.spring.AiService;
import dev.langchain4j.service.spring.AiServiceWiringMode;

@AiService(
        wiringMode = AiServiceWiringMode.EXPLICIT,
        chatModel = "qwenChatModel",
        streamingChatModel = "qwenStreamingChatModel",
        chatMemoryProvider = "chatMemoryProvider",
        tools = "testTools"
)

public interface AiRegisterAssistant {


    @SystemMessage(fromResource = "/registerLost.txt")
    @UserMessage("当前sessionId:{{sessionId}}，用户当前消息:{{message}}")
    LostRegisterOutput registerLost(@V("sessionId") String sessionId, @V("message")String message);

}
