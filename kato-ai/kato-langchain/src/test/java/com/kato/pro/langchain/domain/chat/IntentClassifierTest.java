package com.kato.pro.langchain.domain.chat;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class IntentClassifierTest {

    private static ModelRouter stubRouter(String reply) {
        return new ModelRouter(Map.of()) {
            @Override public Mono<String> route(TaskType t, String s, String u) {
                return Mono.just(reply);
            }
        };
    }

    private static ModelRouter failingRouter() {
        return new ModelRouter(Map.of()) {
            @Override public Mono<String> route(TaskType t, String s, String u) {
                return Mono.error(new RuntimeException("timeout"));
            }
        };
    }

    @Test
    void emptyInput_returnsChitchat() {
        IntentClassifier c = new IntentClassifier(stubRouter(""));
        assertEquals(Intent.CHITCHAT, c.classify("").block());
    }

    @Test
    void parsesToolCall() {
        IntentClassifier c = new IntentClassifier(stubRouter("TOOL_CALL"));
        assertEquals(Intent.TOOL_CALL, c.classify("查订单").block());
    }

    @Test
    void parsesRagOnly() {
        IntentClassifier c = new IntentClassifier(stubRouter("RAG_ONLY"));
        assertEquals(Intent.RAG_ONLY, c.classify("什么是 XX").block());
    }

    @Test
    void errorFallback_defaultsRagOnly() {
        IntentClassifier c = new IntentClassifier(failingRouter());
        assertEquals(Intent.RAG_ONLY, c.classify("hi").block());
    }

    @Test
    void unknownOutput_defaultsRagOnly() {
        IntentClassifier c = new IntentClassifier(stubRouter("garbage output"));
        assertEquals(Intent.RAG_ONLY, c.classify("hi").block());
    }
}
