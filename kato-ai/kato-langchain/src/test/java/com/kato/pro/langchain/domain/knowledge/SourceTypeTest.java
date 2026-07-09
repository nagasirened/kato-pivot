package com.kato.pro.langchain.domain.knowledge;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SourceTypeTest {

    @Test
    void fromCode_known_returnsEnum() {
        assertEquals(SourceType.UPLOAD, SourceType.fromCode("UPLOAD"));
        assertEquals(SourceType.SYNC_PRODUCT, SourceType.fromCode("SYNC_PRODUCT"));
    }

    @Test
    void fromCode_null_returnsDefault() {
        assertEquals(SourceType.UPLOAD, SourceType.fromCode(null));
    }

    @Test
    void fromCode_unknown_throws() {
        assertThrows(IllegalArgumentException.class, () -> SourceType.fromCode("UNKNOWN"));
    }

    @Test
    void code_isStable() {
        assertEquals("UPLOAD", SourceType.UPLOAD.code());
        assertEquals("SYNC_FAQ", SourceType.SYNC_FAQ.code());
    }
}
