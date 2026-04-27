package com.kato.pro.langchain.demo.chat.config;


import com.kato.pro.langchain.demo.chat.service.SeparateAssistant;
import dev.langchain4j.invocation.InvocationContext;
import dev.langchain4j.invocation.InvocationParameters;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.observability.api.event.AiServiceCompletedEvent;
import dev.langchain4j.observability.api.event.AiServiceErrorEvent;
import dev.langchain4j.observability.api.event.AiServiceEvent;
import dev.langchain4j.observability.api.event.AiServiceResponseReceivedEvent;
import dev.langchain4j.observability.api.event.AiServiceStartedEvent;
import dev.langchain4j.observability.api.event.InputGuardrailExecutedEvent;
import dev.langchain4j.observability.api.event.OutputGuardrailExecutedEvent;
import dev.langchain4j.observability.api.event.ToolExecutedEvent;
import dev.langchain4j.observability.api.listener.AiServiceListener;
import dev.langchain4j.service.AiServices;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.function.Consumer;

/**
 * AiServiceListener：挂在 {@link AiServices} 上的可观测性钩子（一次接口调用生命周期），
 * 与贴近 HTTP 的 {@code ChatModelListener} 不同。
 */
@Slf4j
@Configuration
public class AssistantConfiguration {

    @Autowired
    private OpenAiChatModel openAiChatModel;

    @Bean
    private SeparateAssistant separateAssistant(ChatMemoryProvider separateChatMemoryProvider) {
        return AiServices.builder(SeparateAssistant.class)
                .chatModel(openAiChatModel)
                .chatMemoryProvider(separateChatMemoryProvider)
                // @V("temperature") 等通过 InvocationParameters 传入，在此写入 ChatRequest
                .chatRequestTransformer(
                        (chatRequest, context) -> {
                            ChatRequest.Builder builder = chatRequest.toBuilder();

                            InvocationContext invocationContext = (InvocationContext) context;
                            InvocationParameters invocationParameters = invocationContext.invocationParameters();
                            if (invocationParameters.containsKey("temperature")) {
                                Double temperature = invocationContext.invocationParameters().get("temperature");
                                builder.temperature(temperature);
                            }

                            return builder.build();
                        }
                )
                .registerListeners(separateAssistantAiServiceListeners())
                .build();
    }

    /**
     * 每种 {@link AiServiceEvent} 子类型对应一个 Listener；{@link AiServiceListener#getEventClass()} 必须返回该子类型的 Class，框架据此分发事件。
     * <p>
     * 当前模块使用 LangChain4j 1.10.x：包内可监听的事件见下列 {@code List.of}。
     * 升级到 1.12+ 后还会多出 {@code AiServiceRequestIssuedEvent}（ChatRequest 已确定、即将调模型，比 Started 更晚）。
     */
    private static List<AiServiceListener<?>> separateAssistantAiServiceListeners() {
        return List.of(
                // 一次 AiService 方法调用开始：已解析出用户消息（及可选系统消息）；最终 ChatRequest 可能尚未完全定型
                wrapAiServiceListener(AiServiceStartedEvent.class, e -> log.debug(
                        "AiServiceStartedEvent: hasSystemMessage={}, userHasSingleText={}",
                        e.systemMessage().isPresent(),
                        e.userMessage().hasSingleText())),
                // 单次底层 ChatModel 调用返回：事件里同时带有当次 ChatRequest 与 ChatResponse（1.10 无单独的 RequestIssued 事件，可从 request() 取消息条数）
                wrapAiServiceListener(AiServiceResponseReceivedEvent.class, e -> {
                    ChatResponse r = e.response();
                    log.debug(
                            "AiServiceResponseReceivedEvent: requestMessageCount={}, tokenUsage={}, finishReason={}",
                            e.request().messages().size(),
                            r.tokenUsage(),
                            r.finishReason());
                }),
                // 整个 AiService 调用成功结束：可拿到对外返回的 result（若有）
                wrapAiServiceListener(AiServiceCompletedEvent.class, e -> log.debug(
                        "AiServiceCompletedEvent: hasResult={}",
                        e.result().isPresent())),
                // 调用链中发生未处理异常
                wrapAiServiceListener(AiServiceErrorEvent.class, e -> log.warn(
                        "AiServiceErrorEvent",
                        e.error())),
                // 模型触发工具调用且工具已执行完毕：可审计工具名、入参、结果文本
                wrapAiServiceListener(ToolExecutedEvent.class, e -> log.debug(
                        "ToolExecutedEvent: toolName={}, resultTextLength={}",
                        e.request().name(),
                        e.resultText() == null ? 0 : e.resultText().length())),
                // 输入护栏执行结束：例如敏感检测、改写用户消息等
                wrapAiServiceListener(InputGuardrailExecutedEvent.class, e -> log.debug(
                        "InputGuardrailExecutedEvent: guardrailClass={}, duration={}, rewrittenUserMessagePresent={}",
                        e.guardrailClass().getSimpleName(),
                        e.duration(),
                        e.rewrittenUserMessage() != null)),
                // 输出护栏执行结束：例如输出合规检查、改写模型回复等
                wrapAiServiceListener(OutputGuardrailExecutedEvent.class, e -> log.debug(
                        "OutputGuardrailExecutedEvent: guardrailClass={}, duration={}",
                        e.guardrailClass().getSimpleName(),
                        e.duration()))
        );
    }

    /**
     * 构造按事件类型订阅的 {@link AiServiceListener}。框架在注册阶段和每次派发事件时会用到下面两个方法。
     */
    private static <E extends AiServiceEvent> AiServiceListener<E> wrapAiServiceListener(Class<E> eventClass, Consumer<E> onEvent) {
        return new AiServiceListener<E>() {
            /**
             * 作用：声明本监听器要接收哪一种 {@link AiServiceEvent} 子类型。
             * 触发时机：在 {@link AiServices#registerListener} / {@link AiServices#registerListeners} 时由框架读取，
             * 用于建立「事件类型 → 监听器」的映射；之后仅当某次调用产生了该类型的实例时，才会回调对应的 {@link #onEvent}。
             * 须返回非 null 的具体子类（如 {@link AiServiceStartedEvent}.class），不可返回父接口 {@link AiServiceEvent}.class。
             */
            @Override
            public Class<E> getEventClass() {
                return eventClass;
            }

            /**
             * 作用：处理已发生的具体事件（打日志、指标、链路透传等）。
             * 触发时机：在一次 {@code @AiService} 接口方法调用生命周期内，当流程到达该事件对应节点时由框架同步调用；
             * 例如 Started 在调用开端、ResponseReceived 在每次底层模型返回后、Completed 在整次调用成功结束前、Error 在捕获异常时等（与具体事件类型一一对应）。
             */
            @Override
            public void onEvent(E event) {
                onEvent.accept(event);
            }
        };
    }

}
