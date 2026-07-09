package com.kato.pro.langchain.domain.session;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class SessionStatusTest {

    @Test
    void enumCount_is2() {
        assertEquals(2, SessionStatus.values().length);
    }

    @Test
    void containsActiveAndArchived() {
        assertNotNull(SessionStatus.valueOf("ACTIVE"));
        assertNotNull(SessionStatus.valueOf("ARCHIVED"));
    }
}
