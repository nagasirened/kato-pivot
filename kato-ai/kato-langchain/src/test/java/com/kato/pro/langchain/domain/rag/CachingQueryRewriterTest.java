package com.kato.pro.langchain.domain.rag;

import com.kato.pro.langchain.config.RagProperties;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CachingQueryRewriterTest {

    @Test
    void cache_hitsDelegateOnlyOnce() {
        java.util.concurrent.atomic.AtomicInteger calls = new java.util.concurrent.atomic.AtomicInteger();
        QueryRewriter delegate = q -> { calls.incrementAndGet(); return "rewritten:" + q; };
        CachingQueryRewriter c = new CachingQueryRewriter(delegate, new RagProperties());

        assertEquals("rewritten:hello", c.rewrite("hello"));
        assertEquals("rewritten:hello", c.rewrite("hello"));
        assertEquals("rewritten:hello", c.rewrite("hello"));
        assertEquals(1, calls.get());
    }

    @Test
    void cache_differentQueriesCallDelegate() {
        java.util.concurrent.atomic.AtomicInteger calls = new java.util.concurrent.atomic.AtomicInteger();
        QueryRewriter delegate = q -> { calls.incrementAndGet(); return "r:" + q; };
        CachingQueryRewriter c = new CachingQueryRewriter(delegate, new RagProperties());

        c.rewrite("a");
        c.rewrite("b");
        c.rewrite("c");
        assertEquals(3, calls.get());
    }

    @Test
    void cache_nullQuery_returnsEmpty() {
        CachingQueryRewriter c = new CachingQueryRewriter(q -> q, new RagProperties());
        assertEquals("", c.rewrite(null));
    }
}
