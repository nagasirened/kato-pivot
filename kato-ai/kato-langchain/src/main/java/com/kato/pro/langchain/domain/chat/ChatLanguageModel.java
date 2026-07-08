package com.kato.pro.langchain.domain.chat;

import reactor.core.publisher.Mono;

/**
 * 内部 ChatModel 抽象。M2 阶段我们用这个简化接口；M3/M8 接入 LangChain4j 时
 * 适配到 LangChain4j 的 ChatLanguageModel。两者都是 Mono<String>/Mono<ChatResponse> 风格。
 */
public interface ChatLanguageModel {

    /** 模型标识（M2.5 / M3）。 */
    String modelName();

    /** 单轮对话，返回模型文本回复。 */
    Mono<String> chat(String systemPrompt, String userMessage);
}
