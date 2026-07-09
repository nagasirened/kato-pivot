package com.kato.pro.langchain.domain.memory;

import lombok.extern.slf4j.Slf4j;

/**
 * Token 估算器（门面）。
 *
 * 设计（D1=C）：
 *   - 优先用 LangChain4j 的 {@link dev.langchain4j.model.TokenCountEstimator}（基于具体 tokenizer）；
 *   - 若 estimator 抛异常或返回 0，fallback 到 chars/4（OpenAI 经验值，中文略偏高但安全）；
 *   - delegate 可为 null → 走纯 fallback。
 *
 * 类名刻意避开 LangChain4j 同名接口，便于直接 new。
 */
@Slf4j
public class TokenCountEstimator {

    private final dev.langchain4j.model.TokenCountEstimator delegate;
    private final boolean fallback;

    public TokenCountEstimator(dev.langchain4j.model.TokenCountEstimator delegate) {
        this(delegate, true);
    }

    public TokenCountEstimator(dev.langchain4j.model.TokenCountEstimator delegate, boolean fallback) {
        this.delegate = delegate;
        this.fallback = fallback;
    }

    public int estimate(String text) {
        if (text == null || text.isEmpty()) return 0;
        if (delegate != null) {
            try {
                int n = delegate.estimateTokenCountInText(text);
                if (n > 0) return n;
            } catch (Exception e) {
                if (!fallback) throw e;
                log.debug("delegate estimator failed, fallback to chars/4: {}", e.toString());
            }
        }
        return Math.max(1, (text.length() + 3) / 4);
    }

    /** 对多条消息的累计 token 数。 */
    public int estimateAll(Iterable<String> texts) {
        int sum = 0;
        for (String t : texts) sum += estimate(t);
        return sum;
    }
}
