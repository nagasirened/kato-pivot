package com.kato.pro.langchain.domain.memory;

import com.kato.pro.langchain.common.exception.BusinessException;
import com.kato.pro.langchain.config.MemoryProperties;
import com.kato.pro.langchain.domain.chat.ChatLanguageModel;
import com.kato.pro.langchain.domain.chat.ChatMessage;
import com.kato.pro.langchain.domain.chat.ChatSession;
import com.kato.pro.langchain.domain.chat.ModelRouter;
import com.kato.pro.langchain.domain.chat.TaskType;
import com.kato.pro.langchain.domain.session.ChatMessageService;
import com.kato.pro.langchain.domain.session.ChatSessionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MemoryManagerTest {

    private MemoryManager manager;
    private ChatSessionService sessionService;
    private ChatMessageService messageService;

    @BeforeEach
    void setup() {
        sessionService = new ChatSessionService(null) {
            @Override
            public ChatSession getSession(Long id) {
                ChatSession s = new ChatSession();
                s.setId(id);
                s.setUserId(100L);
                s.setSummary("");
                s.setSummaryVersion(0);
                return s;
            }
            @Override
            public boolean updateSummary(Long sid, String newSummary, Integer expectedVersion) {
                return true;
            }
        };

        HistorySummarizer summarizer = makeSummarizer();

        messageService = new ChatMessageService(null) {
            @Override
            public List<ChatMessage> loadRecentMessages(Long sid, int limit) {
                List<ChatMessage> list = new ArrayList<>();
                for (int i = 0; i < 5; i++) {
                    ChatMessage m = new ChatMessage();
                    m.setRole(ChatMessage.Role.USER);
                    m.setContent("a".repeat(10));
                    list.add(m);
                }
                return list;
            }
        };

        manager = new MemoryManager(sessionService, messageService,
                new TokenCountEstimator(null, true),
                new MemoryWindow(),
                summarizer,
                new MemoryProperties());
    }

    @Test
    void prepareContext_nullSessionId_throws() {
        assertThrows(BusinessException.class, () -> manager.prepareContext(null));
    }

    @Test
    void prepareContext_belowTrigger_returnsOriginal() {
        List<ChatMessage> r = manager.prepareContext(1L);
        assertEquals(5, r.size());
    }

    @Test
    void prepareContext_emptyRecent_returnsEmpty() {
        messageService = new ChatMessageService(null) {
            @Override
            public List<ChatMessage> loadRecentMessages(Long sid, int limit) {
                return List.of();
            }
        };
        manager = new MemoryManager(sessionService, messageService,
                new TokenCountEstimator(null, true), new MemoryWindow(),
                makeSummarizer(), new MemoryProperties());
        assertTrue(manager.prepareContext(1L).isEmpty());
    }

    @Test
    void forceSummarize_doesNotThrow() {
        manager.forceSummarize(1L);
    }

    private HistorySummarizer makeSummarizer() {
        ChatLanguageModel model = new ChatLanguageModel() {
            @Override public String modelName() { return "m2.5"; }
            @Override public Mono<String> chat(String systemPrompt, String userMessage) {
                return Mono.just("x");
            }
        };
        Map<TaskType, ChatLanguageModel> table = new EnumMap<>(TaskType.class);
        table.put(TaskType.SIMPLE_SUMMARIZATION, model);
        return new HistorySummarizer(new ModelRouter(table), sessionService,
                new TokenCountEstimator(null, true), new MemoryProperties());
    }
}
