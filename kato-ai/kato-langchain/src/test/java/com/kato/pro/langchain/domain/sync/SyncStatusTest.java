package com.kato.pro.langchain.domain.sync;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * SyncStatus 枚举语义测试。
 *
 * 终态：SUCCESS / PARTIAL / FAILED（非 RUNNING）
 * RUNNING 是唯一非终态。
 */
class SyncStatusTest {

    @Test
    void allValuesPresent() {
        SyncStatus[] values = SyncStatus.values();
        assertEquals(4, values.length);
        assertTrue(java.util.Arrays.asList(values).contains(SyncStatus.RUNNING));
        assertTrue(java.util.Arrays.asList(values).contains(SyncStatus.SUCCESS));
        assertTrue(java.util.Arrays.asList(values).contains(SyncStatus.PARTIAL));
        assertTrue(java.util.Arrays.asList(values).contains(SyncStatus.FAILED));
    }

    @Test
    void running_isNotTerminal() {
        assertFalse(SyncStatus.RUNNING.isTerminal());
    }

    @Test
    void success_partial_failed_areTerminal() {
        assertTrue(SyncStatus.SUCCESS.isTerminal());
        assertTrue(SyncStatus.PARTIAL.isTerminal());
        assertTrue(SyncStatus.FAILED.isTerminal());
    }
}
