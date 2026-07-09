package com.kato.pro.langchain.domain.memory;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TokenCountEstimatorTest {

    private static com.kato.pro.langchain.domain.memory.TokenCountEstimator newFacade(
            dev.langchain4j.model.TokenCountEstimator delegate, boolean fallback) {
        return new com.kato.pro.langchain.domain.memory.TokenCountEstimator(delegate, fallback);
    }

    private static dev.langchain4j.model.TokenCountEstimator stub(
            java.util.function.Function<String, Integer> fn) {
        return new dev.langchain4j.model.TokenCountEstimator() {
            @Override public int estimateTokenCountInText(String s) { return fn.apply(s); }
            @Override public int estimateTokenCountInMessage(dev.langchain4j.data.message.ChatMessage m) { return 0; }
            @Override public int estimateTokenCountInMessages(Iterable<dev.langchain4j.data.message.ChatMessage> msgs) { return 0; }
        };
    }

    @Test
    void estimate_nullOrEmpty_returnsZero() {
        com.kato.pro.langchain.domain.memory.TokenCountEstimator t =
                newFacade(stub(s -> 999), false);
        assertEquals(0, t.estimate(null));
        assertEquals(0, t.estimate(""));
    }

    @Test
    void estimate_usesDelegateWhenReturnsPositive() {
        com.kato.pro.langchain.domain.memory.TokenCountEstimator t =
                newFacade(stub(s -> s == null ? 0 : s.length()), false);
        assertEquals(5, t.estimate("hello"));
    }

    @Test
    void estimate_fallsBackOnException() {
        com.kato.pro.langchain.domain.memory.TokenCountEstimator t =
                newFacade(stub(s -> { throw new RuntimeException("boom"); }), true);
        int n = t.estimate("hello world"); // 11 chars → (11+3)/4 = 3
        assertEquals(3, n);
    }

    @Test
    void estimate_fallsBackOnZero() {
        com.kato.pro.langchain.domain.memory.TokenCountEstimator t =
                newFacade(stub(s -> 0), true);
        int n = t.estimate("abc"); // 3 chars → max(1,(3+3)/4)=1
        assertTrue(n >= 1);
    }

    @Test
    void estimate_all_sums() {
        com.kato.pro.langchain.domain.memory.TokenCountEstimator t =
                newFacade(stub(s -> s == null ? 0 : s.length()), false);
        int total = t.estimateAll(java.util.List.of("hello", "world!"));
        assertEquals(11, total);
    }

    @Test
    void estimate_nullDelegate_usesFallbackDirectly() {
        com.kato.pro.langchain.domain.memory.TokenCountEstimator t =
                newFacade(null, true);
        int n = t.estimate("中文消息测试"); // 6 chars → (6+3)/4=2
        assertEquals(2, n);
    }
}
