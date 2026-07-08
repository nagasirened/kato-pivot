package com.kato.pro.langchain.domain.chat;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TaskTypeTest {

    @Test
    void enumCount_is6() {
        assertEquals(6, TaskType.values().length);
    }

    @Test
    void valueOf_recoversAllConstants() {
        for (TaskType t : TaskType.values()) {
            assertSame(t, TaskType.valueOf(t.name()));
        }
    }

    @Test
    void allSimpleTypes_containSimple() {
        for (TaskType t : TaskType.values()) {
            if (t.name().startsWith("SIMPLE_")) {
                assertTrue(t.name().contains("SIMPLE_"));
            }
        }
    }
}
