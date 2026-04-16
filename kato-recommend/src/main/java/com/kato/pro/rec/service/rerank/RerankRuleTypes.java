package com.kato.pro.rec.service.rerank;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * 写入 {@link com.kato.pro.rec.entity.core.RecommendItem#getRerankTrace()} 的规则类型常量。
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class RerankRuleTypes {

    public static final String STRONG_INSERT = "STRONG_INSERT";
    /** 滑动窗口打散导致相对强插后序列的位置变化 */
    public static final String SLIDING_WINDOW = "SLIDING_WINDOW";
    /** 打散时无法满足窗口约束而放宽选择 */
    public static final String SLIDING_RELAX = "SLIDING_RELAX";
}
