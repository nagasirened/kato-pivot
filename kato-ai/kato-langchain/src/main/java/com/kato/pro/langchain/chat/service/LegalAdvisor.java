package com.kato.pro.langchain.chat.service;


import com.kato.pro.langchain.chat.prompt.LegalPrompt;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.spring.AiService;

import static dev.langchain4j.service.spring.AiServiceWiringMode.EXPLICIT;

@AiService(wiringMode = EXPLICIT, chatModel = "", chatMemoryProvider = "separateChatMemoryProvider")
public interface LegalAdvisor {

    @SystemMessage("你是一位专业的法律顾问，只回答与中国法律有关的问题，其他无关的问题请礼貌拒绝。")
    String answerLegalQuestion(LegalPrompt legalPrompt);

}
