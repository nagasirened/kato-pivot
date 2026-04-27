package com.kato.pro.langchain.demo.chat.service;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.Result;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.spring.AiService;
import dev.langchain4j.service.spring.AiServiceWiringMode;


/**
 * 在AIService中，加入tools的BeanName
 */
@AiService(wiringMode = AiServiceWiringMode.EXPLICIT, chatModel = "", tools = {"calculatorTools"})
public interface CalculateAssistant {

    /**
     * Result<T> 是 LangChain4j 提供的通用返回类型包装器，适用于任何 AiService 方法，无论是否使用 Tool 或 IMMEDIATE 模式。
     * Result<T> 不仅包含模型的文本输出，还封装了元数据，让你能获取更多信息：
     * content()	·   T	                实际内容（模型回复或 Tool 返回值）
     * tokenUsage()	·   TokenUsage	        Token 使用量（输入/输出/总计）
     * sources()	·   List<Source>	    RAG 检索来源（使用 EmbeddingStore 时）
     * finishReason()	FinishReason	    模型停止生成的原因
     * isSuccess()	    boolean	调用是否成功
     */
    Result<String> calculate(@MemoryId String userId, @UserMessage String message);

}
