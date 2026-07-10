package com.kato.pro.langchain.domain.safety;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 链式 filter（D1=C 组合器）。
 *
 * 短路规则：
 *   - 任一 filter 返回 passed=false → 立刻返回 reject（不继续后续）
 *   - 所有 filter 都 passed=true → 返回 pass（hitWords 合并）
 *
 * 降级聚合：fallback=true 只要出现过 → 结果 fallback=true
 */
@Slf4j
public class CompositeSafetyFilter implements ContentSafetyFilter {

    private final List<ContentSafetyFilter> filters;

    public CompositeSafetyFilter(List<ContentSafetyFilter> filters) {
        this.filters = filters == null ? List.of() : List.copyOf(filters);
    }

    @Override
    public String name() { return "composite"; }

    @Override
    public SafetyResult check(String text, SafetyContext ctx) {
        List<String> allHits = new ArrayList<>();
        boolean anyFallback = false;
        List<String> names = new ArrayList<>();
        for (ContentSafetyFilter f : filters) {
            names.add(f.name());
            SafetyResult r;
            try {
                r = f.check(text, ctx);
            } catch (Exception e) {
                log.error("Filter {} threw, treating as fallback: {}", f.name(), e.toString());
                anyFallback = true;
                continue;
            }
            if (r.isFallback()) anyFallback = true;
            if (r.getHitWords() != null) allHits.addAll(r.getHitWords());
            if (!r.isPassed()) {
                return SafetyResult.builder()
                        .passed(false)
                        .hitWords(r.getHitWords())
                        .sanitizedText(r.getSanitizedText())
                        .fallback(anyFallback)
                        .details(Map.of("filter", f.name(), "shortCircuit", true, "chain", names))
                        .build();
            }
        }
        return SafetyResult.builder()
                .passed(true)
                .hitWords(allHits)
                .fallback(anyFallback)
                .details(Map.of("chain", names))
                .build();
    }
}
