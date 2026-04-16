package com.kato.pro.rec.service.rerank.dto;

import cn.hutool.core.collection.CollUtil;
import lombok.Data;

import java.util.HashSet;
import java.util.Set;

/**
 * 强插：在指定 0-based 位置固定放入「labels 与规则 label 集合有交集」的商品（从高分候选池中选取）。
 * <p>配置可同时使用 {@code labels} 数组与单字段 {@code label}（后者并入集合，便于兼容旧配置）。
 */
@Data
public class StrongInsertRule {

    /** 规则关注的 label 集合，与商品 {@code labels} 求交非空即命中 */
    private Set<Integer> labels;
    /** 兼容旧配置：单 label，会与 {@link #labels} 合并为有效集合 */
    private Integer label;
    /** 输出下标，从 0 开始，须小于 topK */
    private Integer position;

    /**
     * 有效 label 集合：{@code labels} 与单值 {@code label} 的并集。
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
