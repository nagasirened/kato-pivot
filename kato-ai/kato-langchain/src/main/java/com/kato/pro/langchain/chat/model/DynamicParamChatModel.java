package com.kato.pro.langchain.chat.model;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.openai.OpenAiChatModel;

import java.util.function.Supplier;

/**
 * 自定义模型包装类，可以添加一些参数用来动态地修改参数
 */
public class DynamicParamChatModel implements ChatModel {

    private final OpenAiChatModel delegate;
    private final Supplier<Double> temperatureSupplier;

    public DynamicParamChatModel(OpenAiChatModel delegate, Supplier<Double> temperatureSupplier) {
        this.delegate = delegate;
        this.temperatureSupplier = temperatureSupplier;
    }

    /**
     * 自定参数
     */
    @Override
    public ChatResponse chat(ChatRequest request) {
        Double dynamicTemp = temperatureSupplier.get();
        if (dynamicTemp != null) {
            ChatRequest chatRequestFacade = request.toBuilder()
                    .temperature(dynamicTemp)
                    .build();
            return delegate.chat(chatRequestFacade);
        }
        return delegate.chat(request);
    }

}
