package com.kato.pro.langchain.domain.safety;

import com.kato.pro.langchain.config.SafetyProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 内存关键词 filter（始终启用，D1=C 兜底）。
 *
 * 命中规则：任一关键词作为子串出现 → 拒绝。
 * 脱敏：把命中关键词替换为等长 *。
 *
 * 性能：O(n*m)（n=文本长度，m=关键词数）；v1 不引入 Aho-Corasick。
 */
@Slf4j
@Component
public class LocalKeywordSafetyFilter implements ContentSafetyFilter {

    private final SafetyProperties properties;

    public LocalKeywordSafetyFilter(SafetyProperties properties) {
        this.properties = properties;
    }

    @Override
    public String name() { return "local-keyword"; }

    @Override
    public SafetyResult check(String text, SafetyContext ctx) {
        if (!properties.isEnabled() || !properties.getLocalKeywords().isEnabled()) {
            return SafetyResult.pass();
        }
        if (text == null || text.isEmpty()) return SafetyResult.pass();
        List<String> words = properties.getLocalKeywords().getDefaultWords();
        if (words == null || words.isEmpty()) return SafetyResult.pass();

        List<String> hits = new ArrayList<>();
        String sanitized = text;
        for (String w : words) {
            if (w == null || w.isEmpty()) continue;
            if (sanitized.contains(w)) {
                hits.add(w);
                sanitized = sanitized.replace(w, "*".repeat(w.length()));
            }
        }
        if (!hits.isEmpty()) {
            log.warn("LocalKeywordSafetyFilter rejected: hits={} dir={} traceId={}",
                    hits, ctx.getDirection(), ctx.getTraceId());
            return SafetyResult.builder()
                    .passed(false).hitWords(hits).sanitizedText(sanitized).fallback(false)
                    .details(Map.of("filter", name(), "direction",
                            ctx.getDirection() == null ? "" : ctx.getDirection().name()))
                    .build();
        }
        return SafetyResult.pass();
    }
}
