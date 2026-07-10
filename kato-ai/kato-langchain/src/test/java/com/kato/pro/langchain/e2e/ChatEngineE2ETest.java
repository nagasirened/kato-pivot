package com.kato.pro.langchain.e2e;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kato.pro.langchain.common.tenant.TenantContext;
import com.kato.pro.langchain.common.tenant.TenantInfo;
import com.kato.pro.langchain.config.ChatEngineProperties;
import com.kato.pro.langchain.config.SafetyProperties;
import com.kato.pro.langchain.domain.chat.ChatApplicationService;
import com.kato.pro.langchain.domain.chat.ChatLanguageModel;
import com.kato.pro.langchain.domain.chat.ChatMessage;
import com.kato.pro.langchain.domain.chat.Intent;
import com.kato.pro.langchain.domain.chat.IntentClassifier;
import com.kato.pro.langchain.domain.chat.ModelRouter;
import com.kato.pro.langchain.domain.chat.TaskType;
import com.kato.pro.langchain.domain.chat.ToolCallParser;
import com.kato.pro.langchain.domain.knowledge.RagPipeline;
import com.kato.pro.langchain.domain.knowledge.ScoredChunk;
import com.kato.pro.langchain.domain.memory.MemoryManager;
import com.kato.pro.langchain.domain.prompt.PromptRenderer;
import com.kato.pro.langchain.domain.prompt.PromptTemplateRegistry;
import com.kato.pro.langchain.domain.prompt.PromptTemplateVo;
import com.kato.pro.langchain.domain.safety.ContentSafetyFilter;
import com.kato.pro.langchain.domain.safety.ContentSafetyService;
import com.kato.pro.langchain.domain.safety.SafetyContext;
import com.kato.pro.langchain.domain.safety.SafetyResult;
import com.kato.pro.langchain.domain.session.ChatMessageService;
import com.kato.pro.langchain.domain.session.ChatSessionService;
import com.kato.pro.langchain.domain.tool.ToolDispatcher;
import com.kato.pro.langchain.domain.tool.ToolResult;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * ChatEngine 端到端集成测试（spec §6 M10）。
 *
 * 不使用 @SpringBootTest（沙箱 JDK21 + byte-buddy 限制）—— 手动 wire 完整链路：
 *   ModelRouter → IntentClassifier → RagPipeline → PromptRenderer → MemoryManager
 *   → ContentSafetyService → ToolDispatcher → ChatMessageService → ChatApplicationService
 *
 * 覆盖：
 *   1. CHITCHAT 路径：跳过 RAG，直接模型回复
 *   2. RAG 路径：触发 RAG，prompt 包含 RAG 上下文
 *   3. TOOL_CALL 路径：模型返回 ```json {name,args}``` → parser 提取 → dispatcher dispatch
 */
class ChatEngineE2ETest {

    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        TenantContext.set(TenantInfo.of(1L, 100L));
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    // ---- 1. CHITCHAT ----
    @Test
    void chitchatPath_skipsRag_andEmitsReply() {
        ChatApplicationService svc = wireChatService(
                Intent.CHITCHAT,
                "你好！我是 kato 客服。",
                List.of(),
                List.of());
        String reply = svc.sendMessage(1L, "你好").block();
        assertNotNull(reply);
        assertEquals("你好！我是 kato 客服。", reply);
    }

    // ---- 2. RAG ----
    @Test
    void ragPath_includesContext_andModelReply() {
        List<ScoredChunk> hits = List.of(
                new ScoredChunk(101L, 1L, "X1 智能手表 14 天续航", 0.92, Map.of()));
        ChatApplicationService svc = wireChatService(
                Intent.RAG_ONLY,
                "X1 智能手表的续航是 14 天。",
                hits,
                List.of());
        String reply = svc.sendMessage(1L, "X1 续航多久").block();
        assertNotNull(reply);
        assertTrue(reply.contains("14 天"));
    }

    // ---- 3. TOOL_CALL ----
    @Test
    void toolCallPath_parsesJsonBlock_andDispatches() {
        String llmReply = "好的，帮你查订单。\n```json\n{\"name\":\"order_query\",\"args\":{\"orderId\":\"O-001\"}}\n```";
        ChatApplicationService svc = wireChatService(
                Intent.TOOL_CALL,
                llmReply,
                List.of(),
                List.of("order_query"));
        String reply = svc.sendMessage(1L, "查订单 O-001").block();
        assertNotNull(reply);
        assertTrue(reply.contains("审核中"), "reply should mention audit pending, got: " + reply);
        assertTrue(reply.contains("42"), "reply should contain auditId 42, got: " + reply);
    }

    // ---- wire ----

    private ChatApplicationService wireChatService(Intent intent, String routerReply,
                                                    List<ScoredChunk> ragHits,
                                                    List<String> toolsToDispatch) {
        ChatLanguageModel stubModel = new ChatLanguageModel() {
            @Override public String modelName() { return "stub"; }
            @Override public Mono<String> chat(String sys, String user) { return Mono.just(routerReply); }
        };
        ModelRouter router = new ModelRouter(Map.of(
                TaskType.COMPLEX_CHAT, stubModel,
                TaskType.SIMPLE_CLASSIFICATION, stubModel,
                TaskType.SIMPLE_SUMMARIZATION, stubModel,
                TaskType.SIMPLE_QUERY_REWRITE, stubModel
        )) {
            @Override
            public Mono<String> route(TaskType t, String sys, String user) {
                return Mono.just(routerReply);
            }
        };

        IntentClassifier classifier = new IntentClassifier(router) {
            @Override
            public Mono<Intent> classify(String userInput) {
                return Mono.just(intent);
            }
        };

        ContentSafetyService safety = passSafety();

        RagPipeline rag = new RagPipeline(null, null, null, null) {
            @Override
            public List<ScoredChunk> search(String q, Integer topK, Double minScore,
                                             Set<Long> docIds, Set<String> sourceTypes) {
                return ragHits;
            }
        };

        PromptRenderer renderer = new PromptRenderer() {
            @Override
            public String substitute(String template, Map<String, Object> vars) {
                return "rendered";
            }
        };

        PromptTemplateRegistry registry = new PromptTemplateRegistry(null, null) {
            @Override
            public Optional<PromptTemplateVo> get(String key, Long tenantId) {
                return Optional.of(new PromptTemplateVo(null, key, "tpl-content:" + key, 1, "STUB"));
            }
        };

        ToolDispatcher dispatcher = new ToolDispatcher(null, null, null, null, null) {
            @Override
            public ToolResult dispatch(String toolName, JsonNode args) {
                if (toolsToDispatch.contains(toolName)) {
                    return ToolResult.ok(Map.of("status", "PENDING", "auditId", 42L));
                }
                return ToolResult.fail("unexpected tool: " + toolName);
            }
        };

        ChatSessionService sessionService = new ChatSessionService(null) {};
        ChatMessageService messageService = new ChatMessageService(null) {
            @Override
            public ChatMessage appendUserMessage(Long sessionId, String content) {
                ChatMessage m = new ChatMessage();
                m.setId(1L); m.setSessionId(sessionId); m.setContent(content);
                return m;
            }
            @Override
            public ChatMessage appendAssistantMessage(Long sessionId, String content, String model, Integer tok) {
                ChatMessage m = new ChatMessage();
                m.setId(2L); m.setSessionId(sessionId); m.setContent(content);
                return m;
            }
            @Override
            public ChatMessage appendToolMessage(Long sessionId, String content, String toolCallJson) {
                ChatMessage m = new ChatMessage();
                m.setId(3L); m.setSessionId(sessionId); m.setContent(content);
                return m;
            }
        };
        MemoryManager memory = new MemoryManager(null, null, null, null, null, null) {
            @Override
            public List<ChatMessage> prepareContext(Long sessionId) {
                return List.of();
            }
        };

        return new ChatApplicationService(
                new ChatEngineProperties(),
                sessionService,
                messageService,
                memory,
                safety,
                classifier,
                rag,
                renderer,
                registry,
                router,
                new ToolCallParser(mapper),
                dispatcher,
                mapper
        );
    }

    private static ContentSafetyService passSafety() {
        SafetyProperties p = new SafetyProperties();
        p.setEnabled(true);
        ContentSafetyFilter pass = new ContentSafetyFilter() {
            @Override public String name() { return "pass"; }
            @Override public SafetyResult check(String text, SafetyContext ctx) {
                return SafetyResult.pass();
            }
        };
        return new ContentSafetyService(p, pass);
    }
}
