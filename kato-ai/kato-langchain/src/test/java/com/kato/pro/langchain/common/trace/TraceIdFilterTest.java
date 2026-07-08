package com.kato.pro.langchain.common.trace;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class TraceIdFilterTest {

    private final TraceIdFilter filter = new TraceIdFilter();

    @AfterEach
    void cleanup() {
        TraceContext.clear();
    }

    @Test
    void doFilter_whenNoHeader_generatesTraceId() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/v1/test");
        MockHttpServletResponse resp = new MockHttpServletResponse();
        AtomicReference<String> mdcAtChainTime = new AtomicReference<>();
        FilterChain chain = (request, response) -> mdcAtChainTime.set(TraceContext.current());

        filter.doFilter(req, resp, chain);

        String traceId = resp.getHeader(TraceIdFilter.HEADER_TRACE_ID);
        assertNotNull(traceId);
        assertEquals(32, traceId.length()); // UUID without dashes
        // 链处理时 MDC 应该有值
        assertEquals(traceId, mdcAtChainTime.get());
        // 链结束后 MDC 应被清空
        assertNull(TraceContext.current());
    }

    @Test
    void doFilter_whenHeaderProvided_reusesTraceId() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/v1/test");
        req.addHeader(TraceIdFilter.HEADER_TRACE_ID, "incoming-trace-123");
        MockHttpServletResponse resp = new MockHttpServletResponse();
        FilterChain chain = (request, response) -> { /* no-op */ };

        filter.doFilter(req, resp, chain);

        assertEquals("incoming-trace-123", resp.getHeader(TraceIdFilter.HEADER_TRACE_ID));
    }

    @Test
    void clear_removesMdcValue() {
        TraceContext.generate();
        assertNotNull(TraceContext.current());
        TraceContext.clear();
        assertNull(TraceContext.current());
    }
}
