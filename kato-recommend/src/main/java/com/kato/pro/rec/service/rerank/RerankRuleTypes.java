package com.kato.pro.rec.service.rerank;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * 重排序链路中写入 {@link com.kato.pro.rec.entity.core.RecommendItem#getRerankTrace()} 的规则类型常量。
 * <p>每个常量对应一种导致商品在重排序后序列中位置变化的业务规则，
 * 便于事后分析、Debug 和链路回放。
 *
 * @see RecommendItem#getRerankTrace()
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class RerankRuleTypes {

    /** 强插（Strong Insert）：商品被固定插入到指定 0-based 位置（{@link RerankStrongInsertService}） */
    public static final String STRONG_INSERT = "STRONG_INSERT";

    /**
     * 滑动窗口打散：因滑动窗口约束导致商品在打散后序列中的位置相对打散前发生变化
     *（{@link RerankSlidingWindowDiversifyService}）。
     */
    public static final String SLIDING_WINDOW = "SLIDING_WINDOW";

    /**
     * 放宽约束：滑动窗口打散时无法在满足 maxInWindow 约束的前提下找到候选商品，
     * 强制取队列首部商品并在 rerankTrace 中记录此类型（{@link RerankSlidingWindowDiversifyService}）。
     */
    public static final String SLIDING_RELAX = "SLIDING_RELAX";
}
