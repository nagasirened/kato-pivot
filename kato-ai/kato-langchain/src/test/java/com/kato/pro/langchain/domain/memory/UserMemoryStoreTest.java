package com.kato.pro.langchain.domain.memory;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class UserMemoryStoreTest {

    @Test
    void noOp_recall_returnsEmpty() {
        UserMemoryStore s = new NoOpUserMemoryStore();
        assertTrue(s.recall(1L, "k").isEmpty());
    }

    @Test
    void noOp_remember_doesNotThrow() {
        UserMemoryStore s = new NoOpUserMemoryStore();
        s.remember(1L, "k", "v");
    }

    @Test
    void noOp_listKeys_returnsEmpty() {
        UserMemoryStore s = new NoOpUserMemoryStore();
        assertTrue(s.listKeys(1L).isEmpty());
    }
}
