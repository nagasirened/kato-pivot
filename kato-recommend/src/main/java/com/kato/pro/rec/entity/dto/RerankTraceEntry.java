package com.kato.pro.rec.entity.dto;

import lombok.Data;

import java.util.Set;

/**
 * 单条重排轨迹：因哪类规则、命中哪组配置 label、落在哪个输出下标（若有）。
 * <p>规则类型字符串与 {@code com.kato.pro.rec.service.rerank.RerankRuleTypes} 中常量一致。
 */
@Data
public class RerankTraceEntry {

    private String ruleType;
    /** 该条规则配置的 label 集合（与商品自身 labels 求交后用于匹配） */
    private Set<Integer> ruleLabels;
    /** 强插或打散写入时的 0-based 输出位置，可为 null */
    private Integer position;
}
