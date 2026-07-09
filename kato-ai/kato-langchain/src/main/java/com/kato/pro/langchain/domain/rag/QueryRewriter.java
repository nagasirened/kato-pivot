package com.kato.pro.langchain.domain.rag;

/**
 * Query 重写器 SPI（D4=C）。
 *
 * 实现：
 *   - v1: LlmQueryRewriter（调 M2.5 / ModelRouter.SIMPLE_QUERY_REWRITE）
 *   - v1: CachingQueryRewriter（装饰器 + Caffeine）
 */
public interface QueryRewriter {

    String rewrite(String rawQuery);

    /** 默认实现：返回原 query（v1 fallback） */
    static QueryRewriter noOp() {
        return rawQuery -> rawQuery == null ? "" : rawQuery;
    }
}
