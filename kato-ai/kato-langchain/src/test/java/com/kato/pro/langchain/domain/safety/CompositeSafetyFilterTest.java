package com.kato.pro.langchain.domain.safety;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CompositeSafetyFilterTest {

    private SafetyContext ctx() {
        return SafetyContext.builder().direction(SafetyContext.Direction.INPUT).tenantId(1L).build();
    }

    private static ContentSafetyFilter alwaysPass(String name) {
        return new ContentSafetyFilter() {
            @Override public String name() { return name; }
            @Override public SafetyResult check(String text, SafetyContext ctx) { return SafetyResult.pass(); }
        };
    }

    private static ContentSafetyFilter alwaysReject(String name, String word) {
        return new ContentSafetyFilter() {
            @Override public String name() { return name; }
            @Override public SafetyResult check(String text, SafetyContext ctx) {
                return SafetyResult.reject(List.of(word), text);
            }
        };
    }

    private static ContentSafetyFilter alwaysFallback(String name) {
        return new ContentSafetyFilter() {
            @Override public String name() { return name; }
            @Override public SafetyResult check(String text, SafetyContext ctx) {
                return SafetyResult.passFallback("simulated");
            }
        };
    }

    @Test
    void emptyChain_passes() {
        CompositeSafetyFilter c = new CompositeSafetyFilter(List.of());
        SafetyResult r = c.check("anything", ctx());
        assertTrue(r.isPassed());
        assertFalse(r.isFallback());
    }

    @Test
    void allPass_returnsPass() {
        CompositeSafetyFilter c = new CompositeSafetyFilter(List.of(
                alwaysPass("a"), alwaysPass("b")));
        assertTrue(c.check("x", ctx()).isPassed());
    }

    @Test
    void shortCircuits_onFirstReject() {
        CompositeSafetyFilter c = new CompositeSafetyFilter(List.of(
                alwaysPass("a"),
                alwaysReject("b", "bad"),
                alwaysPass("c")));
        SafetyResult r = c.check("x", ctx());
        assertFalse(r.isPassed());
        assertEquals(List.of("bad"), r.getHitWords());
        assertEquals(true, r.getDetails().get("shortCircuit"));
    }

    @Test
    void anyFallback_propagates() {
        CompositeSafetyFilter c = new CompositeSafetyFilter(List.of(
                alwaysPass("a"),
                alwaysFallback("b"),
                alwaysPass("c")));
        SafetyResult r = c.check("x", ctx());
        assertTrue(r.isPassed());
        assertTrue(r.isFallback());
    }

    @Test
    void rejectOverridesFallback_inShortCircuit() {
        CompositeSafetyFilter c = new CompositeSafetyFilter(List.of(
                alwaysFallback("a"),
                alwaysReject("b", "bad")));
        SafetyResult r = c.check("x", ctx());
        assertFalse(r.isPassed());
        assertTrue(r.isFallback());
    }

    @Test
    void filterThrows_treatedAsFallbackAndContinues() {
        ContentSafetyFilter throwing = new ContentSafetyFilter() {
            @Override public String name() { return "throw"; }
            @Override public SafetyResult check(String text, SafetyContext ctx) {
                throw new RuntimeException("boom");
            }
        };
        CompositeSafetyFilter c = new CompositeSafetyFilter(List.of(
                throwing, alwaysPass("ok")));
        SafetyResult r = c.check("x", ctx());
        assertTrue(r.isPassed());
        assertTrue(r.isFallback());
    }
}
