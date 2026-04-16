package com.kato.pro.rec.service.rerank.dto;

import cn.hutool.core.collection.CollUtil;
import lombok.Data;

import java.util.HashSet;
import java.util.Set;

/**
 * 滑动窗口打散规则配置。
 * <p>约束：任意连续 {@link #windowSize} 个结果中，与规则 label 集合有交集的商品
 * 至多 {@link #maxInWindow} 个，至少 {@link #minInWindow} 个（可选）。
 *
 * <p>可同时配置 {@code labels}（Set）与单字段 {@code label}（Integer 兼容旧配置），
 * 两者合并为有效集合后生效。
 *
 * @see RerankSlidingWindowDiversifyService
 */
@Data
public class SlidingWindowRule {

    /**
     * 滑动窗口大小（须 > 0 才激活规则）。
     * 表示考察"最近多少个结果"内的约束。
     */
    private Integer windowSize;

    /** 规则关注的 label 集合（与商品 labels 有交集即算命中） */
    private Set<Integer> labels;

    /** 兼容旧配置：单 label，会与 {@link #labels} 合并为有效集合 */
    private Integer label;

    /**
     * 窗口内允许的最多命中商品数（须 >= 0）。
     * 设为 0 表示窗口内不允许出现命中 label 的商品（完全打散）。
     */
    private Integer maxInWindow;

    /** 窗口内匹配 label 的商品数下限，可为 null（null 表示不设下限） */
    private Integer minInWindow;

    /**
     * 有效 label 集合：{@code labels} 与单值 {@code label} 的并集。
     * <p>用于避免同时配置两者时的逻辑遗漏。
     *
     * @return 合并后的 label 集合，永不为 null（空配置时返回空 Set）
     */
    public Set<Integer> effectiveLabels() {
        Set<Integer> out = new HashSet<>();
        if (CollUtil.isNotEmpty(labels)) {
            out.addAll(labels);
        }
        if (label != null) {
            out.add(label);
        }
        return out;
    }
}
