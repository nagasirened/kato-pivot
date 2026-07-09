package com.kato.pro.langchain.domain.knowledge;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class KnowledgeDocStatusTest {

    @Test
    void enumCount_is4() {
        assertEquals(4, KnowledgeDocStatus.values().length);
    }

    @Test
    void containsAll() {
        assertNotNull(KnowledgeDocStatus.valueOf("PENDING"));
        assertNotNull(KnowledgeDocStatus.valueOf("INDEXING"));
        assertNotNull(KnowledgeDocStatus.valueOf("READY"));
        assertNotNull(KnowledgeDocStatus.valueOf("FAILED"));
    }
}
