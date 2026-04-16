package com.kato.pro.rec.service.rerank.dto;

import cn.hutool.core.collection.CollUtil;
import lombok.Data;

import java.util.HashSet;
import java.util.Set;

/**
 * 强插规则配置。
 * <p>在指定 0-based 位置固定放入「商品 labels 与规则 label 集合有交集」的商品
 * （从高分候选池中按分数顺序选取）。
 *
 * <p>配置可同时使用 {@code labels}（Set）与单字段 {@code label}（Integer，兼容旧配置），
 * 两者合并为有效集合后生效。
 *
 * @see RerankStrongInsertService
 */
@Data
public class StrongInsertRule {

    /** 规则关注的 label 集合，与商品 {@code labels} 求交非空即命中 */
    private Set<Integer> labels;

    /** 兼容旧配置：单 label，会与 {@link #labels} 合并为有效集合 */
    private Integer label;

    /**
     * 输出下标（0-based），须小于 topK。
     * 例如 position=0 表示强制将该类商品放在推荐列表的第一位。
     */
    private Integer position;

    /**
     * 有效 label 集合：{@code labels} 与单值 {@code label} 的并集。
     * <p>用于统一判断商品是否命中该强插规则。
     *
     * @return 合并后的 label 集合，永不为 null
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
