package com.kato.pro.langchain.domain.safety;

/**
 * 内容安全 filter SPI（D1=C — 多实现可链式）。
 *
 * 实现：
 *   - LocalKeywordSafetyFilter    (内存关键词匹配，永远可用)
 *   - HttpSensitiveSafetyFilter   (HTTP 调 kato-sensitive-client，故障降级)
 *   - CompositeSafetyFilter       (链式 + 短路)
 */
public interface ContentSafetyFilter {

    /** filter 名（用于日志/诊断） */
    String name();

    /**
     * 检测；filter 自身负责：
     *   - 通过/拒绝的判定
     *   - 降级（外部依赖故障时返回 fallback=true）
     *   - 异常吞掉（不抛给上层）
     */
    SafetyResult check(String text, SafetyContext ctx);
}
