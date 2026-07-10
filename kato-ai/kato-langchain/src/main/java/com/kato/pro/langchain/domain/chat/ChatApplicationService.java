package com.kato.pro.langchain.domain.chat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kato.pro.langchain.common.exception.BusinessException;
import com.kato.pro.langchain.common.exception.ErrorCode;
import com.kato.pro.langchain.common.tenant.TenantContext;
import com.kato.pro.langchain.common.metrics.ChatMetrics;
import com.kato.pro.langchain.config.ChatEngineProperties;
import com.kato.pro.langchain.domain.knowledge.RagPipeline;
import com.kato.pro.langchain.domain.knowledge.ScoredChunk;
import com.kato.pro.langchain.domain.memory.MemoryManager;
import com.kato.pro.langchain.domain.prompt.PromptRenderer;
import com.kato.pro.langchain.domain.prompt.PromptTemplateRegistry;
import com.kato.pro.langchain.domain.safety.ContentSafetyService;
import com.kato.pro.langchain.domain.safety.SafetyResult;
import com.kato.pro.langchain.domain.session.ChatMessageService;
import com.kato.pro.langchain.domain.session.ChatSessionService;
import com.kato.pro.langchain.domain.tool.ToolDispatcher;
import com.kato.pro.langchain.domain.tool.ToolResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * ChatApplicationService — M8 主入口（spec §6 步骤 1-13 编排）。
 *
 *   sendMessage(sessionId, userInput) → Mono<String>（最终回复）
 *
 * 编排：
 *   1. safety.checkInput(userInput) → assertPassedOrThrow
 *   2. messageService.appendUserMessage
 *   3. memory.prepareContext(sessionId)
 *   4. intent.classify(userInput) → Intent
 *   5. rag.search (if RAG/TOOL)
 *   6. promptRenderer.render("rag_chat"|"chitchat", tenantId, vars)
 *   7. modelRouter.route(COMPLEX_CHAT)
 *   8. toolCallParser.parse(reply) → toolDispatcher.dispatch (if present)
 *   9. safety.checkOutput + 持久化
 *
 * 所有调用均在 TenantContext 之内（M1 Filter 已注入）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatApplicationService {

    private final ChatEngineProperties properties;
    @SuppressWarnings("unused")
    private final ChatSessionService sessionService; // M8 留位（未来 session metadata 查询）
    private final ChatMessageService messageService;
    private final MemoryManager memoryManager;
    private final ContentSafetyService safetyService;
    private final IntentClassifier intentClassifier;
    private final RagPipeline ragPipeline;
    private final PromptRenderer promptRenderer;
    private final PromptTemplateRegistry promptRegistry;
    private final ModelRouter modelRouter;
    private final ToolCallParser toolCallParser;
    private final ToolDispatcher toolDispatcher;
    private final ObjectMapper objectMapper;

    public Mono<String> sendMessage(Long sessionId, String userInput) {
        if (sessionId == null) {
            return Mono.error(new BusinessException(ErrorCode.PARAM_INVALID, "sessionId 不能为空"));
        }
        if (userInput == null || userInput.isBlank()) {
            return Mono.error(new BusinessException(ErrorCode.PARAM_INVALID, "userInput 不能为空"));
        }

        // M11 metrics: 入口 + 出口埋点
        long start = System.currentTimeMillis();
        ChatMetrics.onRequest("unknown");

        // Step 1: 入参安全
        SafetyResult inSafety = safetyService.checkInput(userInput);
        safetyService.assertPassedOrThrow(inSafety);

        // Step 2: 持久化 USER 消息
        messageService.appendUserMessage(sessionId, userInput);

        // Step 3: 加载历史
        List<ChatMessage> history = memoryManager.prepareContext(sessionId);

        Long tenantId = TenantContext.requireCurrent().tenantId();

        // Step 4-8: 意图 → RAG → 拼 prompt → 调模型 → tool_call 解析与执行
        return intentClassifier.classify(userInput)
                .flatMap(intent -> {
                    ChatMetrics.onRequest(intent.name());
                    return ragIfNeeded(userInput, intent)
                            .flatMap(ragHits -> {
                                String prompt = buildPrompt(tenantId, history, ragHits, intent, userInput);
                                return modelRouter.route(TaskType.COMPLEX_CHAT,
                                        "你是客服助手。基于上下文回答用户问题。", prompt);
                            })
                            .flatMap(reply -> postProcess(reply, userInput, sessionId));
                })
                .doOnSuccess(reply -> ChatMetrics.onDuration(System.currentTimeMillis() - start))
                .doOnError(err -> {
                    ChatMetrics.onDuration(System.currentTimeMillis() - start);
                    ChatMetrics.onError(err.getClass().getSimpleName());
                });
    }

    /** RAG_ONLY / TOOL_CALL 触发 RAG；CHITCHAT 跳过 */
    private Mono<List<ScoredChunk>> ragIfNeeded(String userInput, Intent intent) {
        if (intent == Intent.CHITCHAT) return Mono.just(List.of());
        return Mono.fromCallable(() -> ragPipeline.search(userInput,
                properties.getRagTopK(), properties.getRagMinScore(), null, null));
    }

    private String buildPrompt(Long tenantId, List<ChatMessage> history,
                               List<ScoredChunk> ragHits, Intent intent, String userInput) {
        Map<String, Object> vars = new HashMap<>();
        vars.put("tenantName", "tenant-" + tenantId);
        vars.put("history", formatHistory(history));
        vars.put("ragContext", formatRag(ragHits));
        vars.put("userInput", userInput);
        String promptKey = (intent == Intent.CHITCHAT) ? "chitchat" : "rag_chat";
        String template = promptRegistry.get(promptKey, tenantId)
                .map(com.kato.pro.langchain.domain.prompt.PromptTemplateVo::content)
                .orElse("");
            return promptRenderer.substitute(template, vars);
    }

    /** 模型回复后：解析 tool_call + 调用 + 输出过滤 + 持久化 */
    private Mono<String> postProcess(String reply, String userInput, Long sessionId) {
        Optional<ParsedToolCall> parsed = properties.isToolCallEnabled()
                ? toolCallParser.parse(reply)
                : Optional.empty();

        if (parsed.isPresent()) {
            ParsedToolCall call = parsed.get();
            log.info("Tool call detected: name={} args={}", call.name(), call.args());
            ToolResult toolResult = toolDispatcher.dispatch(call.name(), call.args());
            String finalReply = assembleToolReply(reply, toolResult);
            String toolResultJson = objectMapper.valueToTree(
                    toolResult.getData() == null ? Map.of() : toolResult.getData()).toString();
            String toolCallJson = "{\"name\":\"" + call.name() +
                    "\",\"args\":" + call.args().toString() + "}";
            messageService.appendToolMessage(sessionId, toolResultJson, toolCallJson);
            return sanitizeAndPersist(finalReply, sessionId);
        }
        return sanitizeAndPersist(reply, sessionId);
    }

    private Mono<String> sanitizeAndPersist(String reply, Long sessionId) {
        SafetyResult outSafety = safetyService.checkOutput(reply);
        String sanitized = outSafety.getSanitizedText() != null ? outSafety.getSanitizedText() : reply;
        messageService.appendAssistantMessage(sessionId, sanitized, "m3", null);
        return Mono.just(sanitized);
    }

    private String assembleToolReply(String originalReply, ToolResult toolResult) {
        if (!toolResult.isSuccess()) {
            return "抱歉，工具执行失败：" + toolResult.getError();
        }
        Map<String, Object> data = toolResult.getData();
        if (data != null && "PENDING".equals(data.get("status"))) {
            return "已为您提交申请（审核中），审核 ID：" + data.get("auditId");
        }
        if (data != null && !data.isEmpty()) {
            return originalReply + "\n\n[工具结果] " + data;
        }
        return originalReply;
    }

    private String formatHistory(List<ChatMessage> history) {
        StringBuilder sb = new StringBuilder();
        for (ChatMessage m : history) {
            if (sb.length() > 0) sb.append("\n");
            sb.append("[").append(m.getRole()).append("] ").append(m.getContent());
        }
        return sb.toString();
    }

    private String formatRag(List<ScoredChunk> hits) {
        if (hits == null || hits.isEmpty()) return "(无相关知识库内容)";
        StringBuilder sb = new StringBuilder();
        for (ScoredChunk c : hits) {
            sb.append("- ").append(c.content()).append("\n");
        }
        return sb.toString();
    }
}
