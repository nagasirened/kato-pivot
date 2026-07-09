package com.kato.pro.langchain.domain.memory;

import com.kato.pro.langchain.domain.chat.ChatMessage;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MemoryWindowTest {

    private static TokenCountEstimator fixed() {
        return new TokenCountEstimator(new dev.langchain4j.model.TokenCountEstimator() {
            @Override public int estimateTokenCountInText(String s) { return s == null ? 0 : s.length(); }
            @Override public int estimateTokenCountInMessage(dev.langchain4j.data.message.ChatMessage m) { return 0; }
            @Override public int estimateTokenCountInMessages(Iterable<dev.langchain4j.data.message.ChatMessage> msgs) { return 0; }
        }, false);
    }

    private List<ChatMessage> msgs(int... lengths) {
        List<ChatMessage> list = new ArrayList<>();
        for (int len : lengths) {
            ChatMessage m = new ChatMessage();
            m.setContent("a".repeat(len));
            list.add(m);
        }
        return list;
    }

    @Test
    void trim_emptyOrNull_returnsEmpty() {
        MemoryWindow w = new MemoryWindow();
        TokenCountEstimator f = fixed();
        assertTrue(w.trim(null, f, 100).isEmpty());
        assertTrue(w.trim(List.of(), f, 100).isEmpty());
    }

    @Test
    void trim_allFit_returnsAll() {
        MemoryWindow w = new MemoryWindow();
        List<ChatMessage> r = w.trim(msgs(10, 20, 30), fixed(), 100);
        assertEquals(3, r.size());
    }

    @Test
    void trim_exceedsMax_keepsTail() {
        MemoryWindow w = new MemoryWindow();
        // 总 100 token，上限 50 → 至少保留最后 1 条
        List<ChatMessage> r = w.trim(msgs(30, 30, 40), fixed(), 50);
        // 从尾开始：40 (total=40, ok) → 30 (total=70, > 50 但 kept 非空 → break) → 结果只保留最后 1 条
        assertEquals(1, r.size());
        assertEquals(40, r.get(0).getContent().length());
    }

    @Test
    void trim_maxZero_keepsLastOne() {
        MemoryWindow w = new MemoryWindow();
        List<ChatMessage> r = w.trim(msgs(10, 20), fixed(), 0);
        assertEquals(1, r.size());
        assertEquals(20, r.get(0).getContent().length());
    }

    @Test
    void trim_preservesAscOrder() {
        MemoryWindow w = new MemoryWindow();
        List<ChatMessage> r = w.trim(msgs(10, 10, 10, 10, 10), fixed(), 25);
        // 5 条各 10 token，从尾开始：10(10), 10(20), 10(30>25 → break) → 保留最后 2 条
        assertEquals(2, r.size());
        assertEquals(10, r.get(0).getContent().length());
        assertEquals(10, r.get(1).getContent().length());
    }
}
