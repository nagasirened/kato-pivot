package com.kato.pro.langchain.domain.chat;

import com.kato.pro.langchain.common.exception.BusinessException;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ModelRouterTest {

    /** Fake model for testing — records what was called. */
    static class FakeModel implements ChatLanguageModel {
        final String name;
        String lastSystem;
        String lastUser;
        FakeModel(String name) { this.name = name; }
        @Override public String modelName() { return name; }
        @Override public Mono<String> chat(String systemPrompt, String userMessage) {
            this.lastSystem = systemPrompt;
            this.lastUser = userMessage;
            return Mono.just("[" + name + "] reply");
        }
    }

    @Test
    void route_simpleTask_dispatchesToSimpleModel() {
        FakeModel m25 = new FakeModel("m2.5");
        FakeModel m3 = new FakeModel("m3");
        Map<TaskType, ChatLanguageModel> table = new HashMap<>();
        table.put(TaskType.SIMPLE_CLASSIFICATION, m25);
        table.put(TaskType.COMPLEX_CHAT, m3);

        ModelRouter router = new ModelRouter(table);

        String result = router.route(TaskType.SIMPLE_CLASSIFICATION, "sys", "hi").block();
        assertEquals("[m2.5] reply", result);
        assertEquals("sys", m25.lastSystem);
        assertEquals("hi", m25.lastUser);
        assertNull(m3.lastUser);
    }

    @Test
    void route_complexTask_dispatchesToComplexModel() {
        FakeModel m25 = new FakeModel("m2.5");
        FakeModel m3 = new FakeModel("m3");
        Map<TaskType, ChatLanguageModel> table = new HashMap<>();
        table.put(TaskType.SIMPLE_CLASSIFICATION, m25);
        table.put(TaskType.COMPLEX_CHAT, m3);

        ModelRouter router = new ModelRouter(table);

        String result = router.route(TaskType.COMPLEX_CHAT, "sys2", "question").block();
        assertEquals("[m3] reply", result);
        assertEquals("question", m3.lastUser);
        assertNull(m25.lastUser);
    }

    @Test
    void route_unregisteredTask_throwsBusinessException() {
        ModelRouter router = new ModelRouter(Map.of());
        BusinessException ex = assertThrows(BusinessException.class,
                () -> router.route(TaskType.SIMPLE_SUMMARIZATION, "sys", "text").block());
        assertTrue(ex.getMessage().contains("SIMPLE_SUMMARIZATION"));
    }

    @Test
    void route_nullTable_throwsNPE() {
        assertThrows(NullPointerException.class, () -> new ModelRouter(null));
    }

    @Test
    void supports_returnsTrueForRegisteredTask() {
        ModelRouter router = new ModelRouter(Map.of(TaskType.COMPLEX_CHAT, new FakeModel("m3")));
        assertTrue(router.supports(TaskType.COMPLEX_CHAT));
        assertFalse(router.supports(TaskType.SIMPLE_CLASSIFICATION));
    }

    @Test
    void routingTable_returnsImmutableSnapshot() {
        FakeModel m = new FakeModel("m3");
        ModelRouter router = new ModelRouter(Map.of(TaskType.COMPLEX_CHAT, m));
        Map<TaskType, ChatLanguageModel> snap = router.routingTable();
        assertEquals(1, snap.size());
        assertThrows(UnsupportedOperationException.class, () -> snap.put(TaskType.SIMPLE_CLASSIFICATION, m));
    }
}
