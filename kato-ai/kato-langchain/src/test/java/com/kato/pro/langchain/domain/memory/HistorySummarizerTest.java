package com.kato.pro.langchain.domain.memory;

import com.kato.pro.langchain.common.exception.BusinessException;
import com.kato.pro.langchain.config.MemoryProperties;
import com.kato.pro.langchain.domain.chat.ChatLanguageModel;
import com.kato.pro.langchain.domain.chat.ChatMessage;
import com.kato.pro.langchain.domain.chat.ChatSession;
import com.kato.pro.langchain.domain.chat.ModelRouter;
import com.kato.pro.langchain.domain.chat.TaskType;
import com.kato.pro.langchain.domain.session.ChatSessionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HistorySummarizerTest {

    private HistorySummarizer summarizer;
    private boolean casReturnsTrue = true;

    @BeforeEach
    void setup() {
        ChatLanguageModel simple = new ChatLanguageModel() {
            @Override public String modelName() { return "m2.5"; }
            @Override public Mono<String> chat(String systemPrompt, String userMessage) {
                String p = userMessage == null ? "" : userMessage;
                return Mono.just("新摘要:" + p.substring(0, Math.min(20, p.length())));
            }
        };
        Map<TaskType, ChatLanguageModel> table = new EnumMap<>(TaskType.class);
        table.put(TaskType.SIMPLE_SUMMARIZATION, simple);
        ModelRouter router = new ModelRouter(table);

        casReturnsTrue = true;
        ChatSessionService svc = new ChatSessionService(null) {
            @Override
            public boolean updateSummary(Long sessionId, String newSummary, Integer expectedVersion) {
                return casReturnsTrue;
            }
        };

        summarizer = new HistorySummarizer(router, svc,
                new TokenCountEstimator(null, true), new MemoryProperties());
    }

    private ChatSession session(Long id, String summary, Integer version) {
        ChatSession s = new ChatSession();
        s.setId(id);
        s.setSummary(summary);
        s.setSummaryVersion(version);
        return s;
    }

    private List<ChatMessage> msgs(String... contents) {
        List<ChatMessage> list = new ArrayList<>();
        for (String c : contents) {
            ChatMessage m = new ChatMessage();
            m.setRole(ChatMessage.Role.USER);
            m.setContent(c);
            list.add(m);
        }
        return list;
    }

    @Test
    void summarize_emptyInput_returnsFalse() {
        assertFalse(summarizer.summarize(session(1L, null, 0), List.of()));
        assertFalse(summarizer.summarize(session(1L, null, 0), null));
    }

    @Test
    void summarize_nullSession_throws() {
        try {
            summarizer.summarize(null, msgs("hi"));
            org.junit.jupiter.api.Assertions.fail("expected BusinessException");
        } catch (BusinessException expected) {
            // ok
        }
    }

    @Test
    void summarize_normalPath_writesAndRecords() {
        boolean ok = summarizer.summarize(session(1L, "", 0), msgs("你好", "我想问订单"));
        assertTrue(ok);
    }

    @Test
    void summarize_secondCallWithinInterval_skipped() {
        assertTrue(summarizer.summarize(session(1L, "", 0), msgs("a")));
        assertFalse(summarizer.summarize(session(1L, "", 0), msgs("b")));
    }

    @Test
    void forceSummarize_bypassesInterval() {
        assertTrue(summarizer.summarize(session(1L, "", 0), msgs("a")));
        assertTrue(summarizer.forceSummarize(session(1L, "", 0), msgs("b")));
    }

    @Test
    void summarize_casFailure_returnsFalse() {
        casReturnsTrue = false;
        assertFalse(summarizer.summarize(session(1L, "", 0), msgs("x")));
    }

    @Test
    void merge_emptyOld_returnsFresh() {
        assertEquals("new", summarizer.merge("", "new"));
        assertEquals("new", summarizer.merge(null, "new"));
    }

    @Test
    void merge_emptyFresh_returnsOld() {
        assertEquals("old", summarizer.merge("old", ""));
        assertEquals("old", summarizer.merge("old", null));
    }

    @Test
    void merge_both_concatenatesWithSeparator() {
        String r = summarizer.merge("old-summary", "fresh-chunk");
        assertTrue(r.contains("old-summary"));
        assertTrue(r.contains("fresh-chunk"));
        assertTrue(r.contains("[续]"));
    }
}
