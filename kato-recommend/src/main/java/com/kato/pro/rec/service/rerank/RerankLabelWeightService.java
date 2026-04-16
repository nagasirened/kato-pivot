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
 */
@Slf4j
@Service
public class RerankLabelWeightService {

    public void applyWeight(List<RecommendItem> items) {
        if (CollUtil.isEmpty(items)) {
            return;
        }
        Map<Integer, Double> labelToWeight = loadLabelWeights();
        if (labelToWeight.isEmpty()) {
            return;
        }
        for (RecommendItem item : items) {
            Set<Integer> labels = item.getLabels();
            if (CollUtil.isEmpty(labels)) {
                continue;
            }
            double factor = 1D;
            for (Integer lab : labels) {
                Double w = labelToWeight.get(lab);
                if (w != null && w > 0D) {
                    factor *= w;
                }
            }
            if (factor != 1D) {
                double base = item.getScore() == null ? 0D : item.getScore();
                item.setScore(base * factor);
            }
        }
    }

    private Map<Integer, Double> loadLabelWeights() {
        String json = SpringUtils.getProperty(
                RecommendConstant.PROP_RERANK_LABEL_WEIGHTS, RecommendConstant.DEFAULT_RERANK_LABEL_WEIGHTS);
        if (CharSequenceUtil.isBlank(json) || "{}".equals(json.trim())) {
            return new HashMap<>();
        }
        try {
            Map<String, Object> raw = JsonUtils.toObject(json, new TypeReference<Map<String, Object>>() {});
            if (raw == null || raw.isEmpty()) {
                return new HashMap<>();
            }
            Map<Integer, Double> out = new HashMap<>();
            for (Map.Entry<String, Object> e : raw.entrySet()) {
                try {
                    int lab = Integer.parseInt(e.getKey().trim());
                    double w = toDouble(e.getValue());
                    if (w > 0D) {
                        out.put(lab, w);
                    }
                } catch (NumberFormatException ex) {
                    log.warn("RerankLabelWeightService#loadLabelWeights, skip bad label key, key={}", e.getKey());
                }
            }
            return out;
        } catch (Exception e) {
            log.warn("RerankLabelWeightService#loadLabelWeights, parse fail, use empty, json={}", json, e);
            return new HashMap<>();
        }
    }

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
