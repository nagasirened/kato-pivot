package com.kato.pro.langchain.domain.safety;

import com.kato.pro.langchain.common.exception.BusinessException;
import com.kato.pro.langchain.common.tenant.TenantContext;
import com.kato.pro.langchain.common.tenant.TenantInfo;
import com.kato.pro.langchain.common.trace.TraceContext;
import com.kato.pro.langchain.config.SafetyProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ContentSafetyServiceTest {

    private ContentSafetyService svc;
    private SafetyProperties props;
    private CompositeSafetyFilter composite;

    @BeforeEach
    void setup() {
        props = new SafetyProperties();
        LocalKeywordSafetyFilter local = new LocalKeywordSafetyFilter(props);
        composite = new CompositeSafetyFilter(List.of(local));
        svc = new ContentSafetyService(props, composite);
        TenantContext.set(TenantInfo.of(1L, 100L));
        TraceContext.set("trace-1");
    }

    @AfterEach
    void cleanup() {
        TenantContext.clear();
        TraceContext.clear();
    }

    @Test
    void globalDisable_skips() {
        props.setEnabled(false);
        SafetyResult r = svc.checkInput("色情");
        assertTrue(r.isPassed());
    }

    @Test
    void reject_rejectsAndCounts() {
        SafetyResult r = svc.checkInput("这是色情内容");
        assertFalse(r.isPassed());
        assertEquals(1L, svc.getRejectCount());
    }

    @Test
    void fallback_incrementsCounter() {
        ContentSafetyFilter fallbackOnly = new ContentSafetyFilter() {
            @Override public String name() { return "test-fallback"; }
            @Override public SafetyResult check(String text, SafetyContext ctx) {
                return SafetyResult.passFallback("x");
            }
        };
        svc = new ContentSafetyService(props, new CompositeSafetyFilter(List.of(fallbackOnly)));
        svc.checkInput("clean");
        assertEquals(1L, svc.getFallbackCount());
    }

    @Test
    void checkOutput_sameBehavior() {
        SafetyResult r = svc.checkOutput("包含赌博");
        assertFalse(r.isPassed());
    }

    @Test
    void assertPassedOrThrow_passesSafe() {
        svc.assertPassedOrThrow(SafetyResult.pass());
    }

    @Test
    void assertPassedOrThrow_throwsOnReject() {
        SafetyResult r = SafetyResult.reject(List.of("色情"), "**");
        BusinessException ex = assertThrows(BusinessException.class, () -> svc.assertPassedOrThrow(r));
        assertTrue(ex.getMessage().contains("内容安全检测"));
    }
}
