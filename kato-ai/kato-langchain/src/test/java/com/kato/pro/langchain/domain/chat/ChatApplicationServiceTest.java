package com.kato.pro.langchain.domain.chat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kato.pro.langchain.common.exception.BusinessException;
import com.kato.pro.langchain.common.tenant.TenantContext;
import com.kato.pro.langchain.common.tenant.TenantInfo;
import com.kato.pro.langchain.config.ChatEngineProperties;
import com.kato.pro.langchain.config.SafetyProperties;
import com.kato.pro.langchain.domain.knowledge.RagPipeline;
import com.kato.pro.langchain.domain.knowledge.ScoredChunk;
import com.kato.pro.langchain.domain.memory.MemoryManager;
import com.kato.pro.langchain.domain.prompt.PromptRenderer;
import com.kato.pro.langchain.domain.prompt.PromptTemplateRegistry;
import com.kato.pro.langchain.domain.prompt.YamlPromptSource;
import com.kato.pro.langchain.domain.prompt.PromptTemplateVo;
import com.kato.pro.langchain.domain.safety.ContentSafetyService;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * ChatApplicationService 参数校验与编排短路测试。
 *
 * 不使用 Mockito（sandbox JDK21 + byte-buddy broken）；全部用匿名内部类手写 stub。
 */
class ChatApplicationServiceTest {

    @BeforeEach
    void setUp() { TenantContext.set(TenantInfo.of(1L, 100L)); }
    @AfterEach
    void tearDown() { TenantContext.clear(); }

    @Test
    void nullSessionId_throws() {
        ChatApplicationService svc = newSvc(Intent.CHITCHAT, "hi", false);
        assertThrows(BusinessException.class, () -> svc.sendMessage(null, "x").block());
    }

    @Test
    void nullUserInput_throws() {
        ChatApplicationService svc = newSvc(Intent.CHITCHAT, "hi", false);
        assertThrows(BusinessException.class, () -> svc.sendMessage(1L, null).block());
    }

    @Test
    void blankUserInput_throws() {
        ChatApplicationService svc = newSvc(Intent.CHITCHAT, "hi", false);
        assertThrows(BusinessException.class, () -> svc.sendMessage(1L, "  ").block());
    }

    @Test
    void chitchatPath_emitsReply() {
        ChatApplicationService svc = newSvc(Intent.CHITCHAT, "你好！有什么可以帮您？", false);
        assertEquals("你好！有什么可以帮您？", svc.sendMessage(1L, "你好").block());
    }

    @Test
    void ragOnlyPath_assemblesPromptAndCallsRouter() {
        ChatApplicationService svc = newSvc(Intent.RAG_ONLY, "rag reply", true);
        assertEquals("rag reply", svc.sendMessage(1L, "什么是 XX").block());
    }

    @Test
    void safetyReject_throws() {
        // safetyService stub：reject 所有输入
        ContentSafetyService rejecting = new ContentSafetyService(safetyProps(), new com.kato.pro.langchain.domain.safety.ContentSafetyFilter() {
            @Override public String name() { return "stub"; }
            @Override public SafetyResult check(String text, com.kato.pro.langchain.domain.safety.SafetyContext ctx) {
                return SafetyResult.reject(List.of("bad"), "改写");
            }
        });
        ChatApplicationService svc = new ChatApplicationService(
                new ChatEngineProperties(),
                new ChatSessionServiceStub(),
                new ChatMessageServiceStub(),
                new MemoryManagerStub(List.of()),
                rejecting,
                new IntentClassifier(new ModelRouter(Map.of()) {
                    @Override public Mono<String> route(TaskType t, String s, String u) { return Mono.just("ignored"); }
                }),
                new RagPipelineStub(List.of()),
                new PromptRendererStub(),
                new PromptTemplateRegistryStub(),
                new ModelRouter(Map.of()) {
                    @Override public Mono<String> route(TaskType t, String s, String u) { return Mono.just("ignored"); }
                },
                new ToolCallParser(new ObjectMapper()),
                new ToolDispatcherStub(),
                new ObjectMapper()
        );
        assertThrows(BusinessException.class, () -> svc.sendMessage(1L, "trigger").block());
    }

    // ---- helpers ----
    private static ChatApplicationService newSvc(Intent intent, String routerReply, boolean withRag) {
        return new ChatApplicationService(
                new ChatEngineProperties(),
                new ChatSessionServiceStub(),
                new ChatMessageServiceStub(),
                new MemoryManagerStub(List.of()),
                newPassSafety(),
                new IntentClassifier(new ModelRouter(Map.of()) {
                    @Override public Mono<String> route(TaskType t, String s, String u) { return Mono.just(intent.name()); }
                }),
                new RagPipelineStub(withRag ? List.of(new ScoredChunk(1L, 1L, "kb content", 0.9, Map.of())) : List.of()),
                new PromptRendererStub(),
                new PromptTemplateRegistryStub(),
                new ModelRouter(Map.of()) {
                    @Override public Mono<String> route(TaskType t, String s, String u) { return Mono.just(routerReply); }
                },
                new ToolCallParser(new ObjectMapper()),
                new ToolDispatcherStub(),
                new ObjectMapper()
        );
    }

    private static ContentSafetyService newPassSafety() {
        return new ContentSafetyService(safetyProps(), new com.kato.pro.langchain.domain.safety.ContentSafetyFilter() {
            @Override public String name() { return "stub"; }
            @Override public SafetyResult check(String text, com.kato.pro.langchain.domain.safety.SafetyContext ctx) {
                return SafetyResult.pass();
            }
        });
    }

    private static SafetyProperties safetyProps() {
        SafetyProperties p = new SafetyProperties();
        p.setEnabled(true);
        return p;
    }

    // ---- 手写 stub ----
    static class ChatSessionServiceStub extends ChatSessionService {
        ChatSessionServiceStub() { super(null); }
    }
    static class ChatMessageServiceStub extends ChatMessageService {
        ChatMessageServiceStub() { super(null); }
        @Override public com.kato.pro.langchain.domain.chat.ChatMessage appendUserMessage(Long sid, String content) {
            return null;  // 不真写库
        }
        @Override public com.kato.pro.langchain.domain.chat.ChatMessage appendAssistantMessage(Long sid, String content, String model, Integer tok) {
            return null;
        }
        @Override public com.kato.pro.langchain.domain.chat.ChatMessage appendToolMessage(Long sid, String content, String toolCallJson) {
            return null;
        }
    }
    static class MemoryManagerStub extends MemoryManager {
        private final List<ChatMessage> messages;
        MemoryManagerStub(List<ChatMessage> m) {
            super(null, null, null, null, null, null);
            this.messages = m;
        }
        @Override public List<ChatMessage> prepareContext(Long sid) {
            return messages;
        }
    }
    static class RagPipelineStub extends RagPipeline {
        private final List<ScoredChunk> hits;
        RagPipelineStub(List<ScoredChunk> h) {
            super(null, null, null, null);
            this.hits = h;
        }
        @Override public List<ScoredChunk> search(String q, Integer topK, Double minScore,
                                                 java.util.Set<Long> docIds, java.util.Set<String> sourceTypes) {
            return hits;
        }
    }
    static class PromptTemplateRegistryStub extends PromptTemplateRegistry {
        PromptTemplateRegistryStub() {
            super(new YamlPromptSource(new org.springframework.core.io.DefaultResourceLoader()),
                  () -> java.util.List.of());
        }
    }
    static class PromptRendererStub extends PromptRenderer {
        @Override public String substitute(String template, Map<String, Object> vars) {
            return "rendered-prompt";
        }
    }
    static class ToolDispatcherStub extends ToolDispatcher {
        ToolDispatcherStub() {
            super(null, null, null, null, null);
        }
        @Override public ToolResult dispatch(String name, com.fasterxml.jackson.databind.JsonNode args) {
            return ToolResult.ok(Map.of("status", "PENDING", "auditId", 1L));
        }
    }
}
