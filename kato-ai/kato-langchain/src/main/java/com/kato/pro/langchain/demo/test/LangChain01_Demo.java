package com.kato.pro.langchain.demo.test;

import dev.langchain4j.model.openai.OpenAiChatModel;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class LangChain01_Demo {

    @Autowired
    private OpenAiChatModel openAiChatModel;

    @Test
    public void testLangChainSpringBoot() {
        String response = openAiChatModel.chat("你能做什么？");
        System.out.println(response);
    }

    @Test
    public void testLangChainPrimitive() {
        OpenAiChatModel model = OpenAiChatModel.builder()
                .modelName("gpt-4o-mini")
                .baseUrl("http://langchain4j.dev/demo/openai/v1")
                .apiKey("demo")
                .build();

        String response = model.chat("你能做什么？");
        System.out.println(response);
    }

}
