package com.kato.pro.rec.service.rerank;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.text.CharSequenceUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.kato.pro.base.util.SpringUtils;
import com.kato.pro.common.utils.JsonUtils;
import com.kato.pro.rec.entity.constant.RecommendConstant;
import com.kato.pro.rec.entity.core.RecommendItem;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 按配置对命中指定 label 的商品分数做乘法加权（多 label 命中时倍率连乘）。
 * <p>例：商品同时命中 label=1（权重 1.5）和 label=2（权重 1.2），则最终分数 = 原分数 * 1.5 * 1.2。
 * <p>配置来源为 {@link RecommendConstant#PROP_RERANK_LABEL_WEIGHTS}，格式为 JSON map，如：{"1": 1.5, "2": 1.2}
 */
@Slf4j
@Service
public class RerankLabelWeightService {

    /**
     * 对商品列表中命中配置 label 的商品做乘法加权。
     *
     * @param items 待处理的商品列表（会被直接修改 score 字段）
     */
    public void applyWeight(List<RecommendItem> items) {
        // 空列表或无配置时直接返回，避免后续无效遍历
        if (CollUtil.isEmpty(items)) {
            return;
        }
        // 从配置加载 label -> 权重映射表
        Map<Integer, Double> labelToWeight = loadLabelWeights();
        if (labelToWeight.isEmpty()) {
            return;
        }
        for (RecommendItem item : items) {
            Set<Integer> labels = item.getLabels();
            // 无 label 的商品不参与加权，跳过
            if (CollUtil.isEmpty(labels)) {
                continue;
            }
            // 从 1D 开始连乘，命中多个 label 时效果叠加
            double factor = 1D;
            for (Integer lab : labels) {
                Double w = labelToWeight.get(lab);
                // 权重须 > 0 才参与乘法，null 或负数/零均忽略
                if (w != null && w > 0D) {
                    factor *= w;
                }
            }
            // factor == 1D 表示没有任何有效命中，无需修改分数
            if (factor != 1D) {
                double base = item.getScore() == null ? 0D : item.getScore();
                item.setScore(base * factor);
            }
        }
    }

    /**
     * 从 Spring 配置中加载 label 权重映射表。
     *
     * @return Map，key 为 label（Integer），value 为权重（Double）
     */
    private Map<Integer, Double> loadLabelWeights() {
        String json = SpringUtils.getProperty(
                RecommendConstant.PROP_RERANK_LABEL_WEIGHTS, RecommendConstant.DEFAULT_RERANK_LABEL_WEIGHTS);
        // 空或空对象直接返回空 map，避免后续解析开销
        if (CharSequenceUtil.isBlank(json) || "{}".equals(json.trim())) {
            return new HashMap<>();
        }
        try {
            // JSON 结构为 {"labelInt": weightDouble, ...}
            Map<String, Object> raw = JsonUtils.toObject(json, new TypeReference<Map<String, Object>>() {});
            if (raw == null || raw.isEmpty()) {
                return new HashMap<>();
            }
            Map<Integer, Double> out = new HashMap<>();
            for (Map.Entry<String, Object> e : raw.entrySet()) {
                try {
                    // label 须为整数字符串
                    int lab = Integer.parseInt(e.getKey().trim());
                    double w = toDouble(e.getValue());
                    // 只保留有效正权重，负数或零视为无效配置
                    if (w > 0D) {
                        out.put(lab, w);
                    }
                } catch (NumberFormatException ex) {
                    // 非整数字符串跳过，防止脏配置导致整表解析失败
                    log.warn("RerankLabelWeightService#loadLabelWeights, skip bad label key, key={}", e.getKey());
                }
            }
            return out;
        } catch (Exception e) {
            // 解析异常返回空 map，使重排序流程降级为不过滤（不过滤总比崩溃好）
            log.warn("RerankLabelWeightService#loadLabelWeights, parse fail, use empty, json={}", json, e);
            return new HashMap<>();
        }
    }

    /**
     * 将任意类型值转为 double，null 或无法解析时返回默认值 1D。
     */
    private static double toDouble(Object v) {
        if (v == null) {
            return 1D;
        }
        if (v instanceof Number) {
            return ((Number) v).doubleValue();
        }
        return Double.parseDouble(v.toString().trim());
    }
}
