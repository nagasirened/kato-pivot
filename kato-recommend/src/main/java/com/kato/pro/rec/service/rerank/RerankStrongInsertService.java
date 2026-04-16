package com.kato.pro.rec.service.rerank;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.text.CharSequenceUtil;
import com.kato.pro.base.util.SpringUtils;
import com.kato.pro.common.utils.JsonUtils;
import com.kato.pro.rec.entity.constant.RecommendConstant;
import com.kato.pro.rec.entity.core.RecommendItem;
import com.kato.pro.rec.service.rerank.dto.StrongInsertRule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 强插：在 topK 输出长度内，将「labels 与规则 label 集合有交集」的商品固定到配置的 0-based 位置（其余位置按分数顺序从候选池填充）。
 */
@Slf4j
@Service
public class RerankStrongInsertService {

    public List<RecommendItem> apply(List<RecommendItem> sortedDedupedPool, int topK) {
        if (topK <= 0 || CollUtil.isEmpty(sortedDedupedPool)) {
            return new ArrayList<>();
        }
        List<StrongInsertRule> rules = loadRules();
        if (CollUtil.isEmpty(rules)) {
            return takeHead(sortedDedupedPool, topK);
        }
        rules.sort(Comparator.comparingInt(r -> r.getPosition() == null ? Integer.MAX_VALUE : r.getPosition()));

        RecommendItem[] slots = new RecommendItem[topK];
        Set<Integer> placedIds = new HashSet<>();

        for (StrongInsertRule rule : rules) {
            Set<Integer> ruleLabels = rule.effectiveLabels();
            if (CollUtil.isEmpty(ruleLabels) || rule.getPosition() == null) {
                continue;
            }
            int pos = rule.getPosition();
            if (pos < 0 || pos >= topK) {
                log.warn("RerankStrongInsertService#apply, skip rule, invalid position={}, topK={}", pos, topK);
                continue;
            }
            if (slots[pos] != null) {
                continue;
            }
            RecommendItem chosen = pickFirstMatchingLabels(sortedDedupedPool, ruleLabels, placedIds);
            if (chosen != null) {
                slots[pos] = chosen;
                chosen.appendRerankTrace(RerankRuleTypes.STRONG_INSERT, new HashSet<>(ruleLabels), pos);
                if (chosen.getItemId() != null) {
                    placedIds.add(chosen.getItemId());
                }
            }
        }

        int scan = 0;
        for (int p = 0; p < topK; p++) {
            if (slots[p] != null) {
                continue;
            }
            while (scan < sortedDedupedPool.size()) {
                RecommendItem it = sortedDedupedPool.get(scan++);
                Integer id = it.getItemId();
                if (id != null && placedIds.contains(id)) {
                    continue;
                }
                slots[p] = it;
                if (id != null) {
                    placedIds.add(id);
                }
                break;
            }
        }

        List<RecommendItem> out = new ArrayList<>(topK);
        for (RecommendItem s : slots) {
            if (s != null) {
                out.add(s);
            }
        }
        return out;
    }

    private static List<RecommendItem> takeHead(List<RecommendItem> pool, int topK) {
        int n = Math.min(topK, pool.size());
        return new ArrayList<>(pool.subList(0, n));
    }

    private static boolean labelsIntersectRule(Set<Integer> itemLabels, Set<Integer> ruleLabels) {
        if (CollUtil.isEmpty(itemLabels) || CollUtil.isEmpty(ruleLabels)) {
            return false;
        }
        return !Collections.disjoint(itemLabels, ruleLabels);
    }

    private static RecommendItem pickFirstMatchingLabels(
            List<RecommendItem> sortedPool, Set<Integer> ruleLabels, Set<Integer> placedIds) {
        for (RecommendItem it : sortedPool) {
            Integer id = it.getItemId();
            if (id != null && placedIds.contains(id)) {
                continue;
            }
            if (labelsIntersectRule(it.getLabels(), ruleLabels)) {
                return it;
            }
        }
        return null;
    }

    private List<StrongInsertRule> loadRules() {
        String json = SpringUtils.getProperty(
                RecommendConstant.PROP_RERANK_STRONG_INSERTS, RecommendConstant.DEFAULT_RERANK_STRONG_INSERTS);
        if (CharSequenceUtil.isBlank(json) || "[]".equals(json.trim())) {
            return new ArrayList<>();
        }
        try {
            List<StrongInsertRule> list = JsonUtils.toList(json, StrongInsertRule.class);
            return list == null ? new ArrayList<>() : list;
        } catch (Exception e) {
            log.warn("RerankStrongInsertService#loadRules, parse fail, json={}", json, e);
            return new ArrayList<>();
        }
    }
}
