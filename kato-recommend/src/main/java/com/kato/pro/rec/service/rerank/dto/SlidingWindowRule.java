package com.kato.pro.rec.service.rerank.dto;

import cn.hutool.core.collection.CollUtil;
import lombok.Data;

import java.util.HashSet;
import java.util.Set;

/**
 * 滑动窗口打散：任意连续 {@link #windowSize} 个结果中，与规则 label 集合有交集的商品至多 {@link #maxInWindow} 个。
 * <p>可同时配置 {@code labels} 与单字段 {@code label}（后者并入集合）。
 */
@Data
public class SlidingWindowRule {

    private Integer windowSize;
    /** 规则关注的 label 集合 */
    private Set<Integer> labels;
    /** 兼容旧配置：单 label */
    private Integer label;
    private Integer maxInWindow;

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
