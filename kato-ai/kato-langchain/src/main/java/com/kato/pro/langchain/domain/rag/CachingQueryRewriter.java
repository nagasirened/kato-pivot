package com.kato.pro.langchain.domain.rag;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.kato.pro.langchain.config.RagProperties;

import java.time.Duration;
import java.util.Objects;

/**
 * Query 重写缓存装饰器（Caffeine）。
 *
 * key = 原 query（hashCode）；value = 重写结果。
 * 默认 TTL = rag.query-rewriter.cache-ttl-seconds（24h）。
 */
public class CachingQueryRewriter implements QueryRewriter {

    private final QueryRewriter delegate;
    private final Cache<Integer, String> cache;

    public CachingQueryRewriter(QueryRewriter delegate, RagProperties props) {
        this.delegate = delegate;
        long ttl = props.getQueryRewriter().getCacheTtlSeconds();
        this.cache = Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofSeconds(ttl))
                .maximumSize(10_000)
                .build();
    }

    @Override
    public String rewrite(String rawQuery) {
        if (rawQuery == null) return "";
        Integer key = Objects.hash(rawQuery);
        String cached = cache.getIfPresent(key);
        if (cached != null) return cached;
        String result = delegate.rewrite(rawQuery);
        cache.put(key, result);
        return result;
    }
}
