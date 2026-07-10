package com.kato.pro.langchain.domain.safety;

import com.kato.pro.langchain.config.SafetyProperties;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LocalKeywordSafetyFilterTest {

    private SafetyContext ctx() {
        return SafetyContext.builder()
                .direction(SafetyContext.Direction.INPUT)
                .tenantId(1L).traceId("t1").build();
    }

    @Test
    void rejects_containsKeyword() {
        SafetyProperties p = new SafetyProperties();
        LocalKeywordSafetyFilter f = new LocalKeywordSafetyFilter(p);
        SafetyResult r = f.check("你看这个色情内容", ctx());
        assertFalse(r.isPassed());
        assertTrue(r.getHitWords().contains("色情"));
        assertTrue(r.getSanitizedText().contains("**"));
    }

    @Test
    void passes_cleanText() {
        SafetyProperties p = new SafetyProperties();
        LocalKeywordSafetyFilter f = new LocalKeywordSafetyFilter(p);
        SafetyResult r = f.check("你好，请问订单 123 怎么查", ctx());
        assertTrue(r.isPassed());
    }

    @Test
    void globalDisable_skipsCheck() {
        SafetyProperties p = new SafetyProperties();
        p.setEnabled(false);
        LocalKeywordSafetyFilter f = new LocalKeywordSafetyFilter(p);
        assertTrue(f.check("色情色情", ctx()).isPassed());
    }

    @Test
    void keywordListDisable_skipsCheck() {
        SafetyProperties p = new SafetyProperties();
        p.getLocalKeywords().setEnabled(false);
        LocalKeywordSafetyFilter f = new LocalKeywordSafetyFilter(p);
        assertTrue(f.check("色情", ctx()).isPassed());
    }

    @Test
    void emptyKeywordList_passesAll() {
        SafetyProperties p = new SafetyProperties();
        p.getLocalKeywords().setDefaultWords(java.util.List.of());
        LocalKeywordSafetyFilter f = new LocalKeywordSafetyFilter(p);
        assertTrue(f.check("色情", ctx()).isPassed());
    }

    @Test
    void multipleHits_replacedAll() {
        SafetyProperties p = new SafetyProperties();
        p.getLocalKeywords().setDefaultWords(java.util.List.of("色情", "赌博"));
        LocalKeywordSafetyFilter f = new LocalKeywordSafetyFilter(p);
        SafetyResult r = f.check("赌博和色情内容", ctx());
        assertFalse(r.isPassed());
        assertEquals(2, r.getHitWords().size());
        assertEquals("**和**内容", r.getSanitizedText());
    }

    @Test
    void nullOrEmptyText_passes() {
        SafetyProperties p = new SafetyProperties();
        LocalKeywordSafetyFilter f = new LocalKeywordSafetyFilter(p);
        assertTrue(f.check(null, ctx()).isPassed());
        assertTrue(f.check("", ctx()).isPassed());
    }
}
