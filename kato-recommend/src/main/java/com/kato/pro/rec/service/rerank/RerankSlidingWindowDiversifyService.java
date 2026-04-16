package com.kato.pro.rec.service.rerank;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.text.CharSequenceUtil;
import com.kato.pro.base.util.SpringUtils;
import com.kato.pro.common.utils.JsonUtils;
import com.kato.pro.rec.entity.constant.RecommendConstant;
import com.kato.pro.rec.entity.core.RecommendItem;
import com.kato.pro.rec.service.rerank.dto.SlidingWindowRule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 滑动窗口打散：在保持 multiset 不变的前提下重排，使任意长度为 W 的连续子序列中，
 * 与规则 label 集合有交集的商品数不超过配置上限；无法满足时取队首并打 warn。
 */
@Slf4j
@Service
public class RerankSlidingWindowDiversifyService {

    public List<RecommendItem> diversify(List<RecommendItem> ordered, int topK) {
        if (CollUtil.isEmpty(ordered)) {
            return new ArrayList<>();
        }
        SlidingWindowRule rule = loadRule();
        if (rule == null || !isActive(rule)) {
            return trim(ordered, topK);
        }

        Set<Integer> ruleLabels = rule.effectiveLabels();
        List<RecommendItem> before = new ArrayList<>(ordered);
        List<RecommendItem> candidates = new ArrayList<>(ordered);
        List<RecommendItem> out = new ArrayList<>(Math.min(ordered.size(), topK));

        while (!candidates.isEmpty() && out.size() < topK) {
            RecommendItem pick = null;
            int pickIndex = -1;
            for (int i = 0; i < candidates.size(); i++) {
                RecommendItem c = candidates.get(i);
                if (!wouldViolate(out, c, ruleLabels, rule.getWindowSize(), rule.getMaxInWindow())) {
                    pick = c;
                    pickIndex = i;
                    break;
                }
            }
            if (pick == null) {
                pick = candidates.get(0);
                pickIndex = 0;
                log.warn(
                        "RerankSlidingWindowDiversifyService#diversify, relax constraint, ruleLabels={}, windowSize={}",
                        ruleLabels,
                        rule.getWindowSize());
                pick.appendRerankTrace(RerankRuleTypes.SLIDING_RELAX, new HashSet<>(ruleLabels), out.size());
            }
            candidates.remove(pickIndex);
            out.add(pick);
        }

        tagSlidingRepositions(before, out, ruleLabels);
        return out;
    }

    /**
     * 相对打散前序列发生位置变化的商品，追加 {@link RerankRuleTypes#SLIDING_WINDOW} 轨迹。
     */
    private static void tagSlidingRepositions(List<RecommendItem> before, List<RecommendItem> after, Set<Integer> ruleLabels) {
        if (CollUtil.isEmpty(ruleLabels)) {
            return;
        }
        Map<Integer, Integer> idToIndexBefore = new HashMap<>();
        for (int i = 0; i < before.size(); i++) {
            RecommendItem it = before.get(i);
            if (it.getItemId() != null) {
                idToIndexBefore.put(it.getItemId(), i);
            }
        }
        Set<Integer> labelSnapshot = new HashSet<>(ruleLabels);
        for (int j = 0; j < after.size(); j++) {
            RecommendItem it = after.get(j);
            if (it.getItemId() == null) {
                continue;
            }
            Integer oldIdx = idToIndexBefore.get(it.getItemId());
            if (oldIdx != null && !oldIdx.equals(j)) {
                it.appendRerankTrace(RerankRuleTypes.SLIDING_WINDOW, labelSnapshot, j);
            }
        }
    }

    private static List<RecommendItem> trim(List<RecommendItem> ordered, int topK) {
        int n = Math.min(topK, ordered.size());
        return new ArrayList<>(ordered.subList(0, n));
    }

    private static boolean isActive(SlidingWindowRule rule) {
        return rule.getWindowSize() != null
                && rule.getWindowSize() > 0
                && CollUtil.isNotEmpty(rule.effectiveLabels())
                && rule.getMaxInWindow() != null
                && rule.getMaxInWindow() >= 0;
    }

    private static boolean hitsRuleLabels(RecommendItem it, Set<Integer> ruleLabels) {
        return it.getLabels() != null && !Collections.disjoint(it.getLabels(), ruleLabels);
    }

    private static boolean wouldViolate(
            List<RecommendItem> out,
            RecommendItem cand,
            Set<Integer> ruleLabels,
            int windowSize,
            int maxInWindow) {
        if (!hitsRuleLabels(cand, ruleLabels)) {
            return false;
        }
        int from = Math.max(0, out.size() - (windowSize - 1));
        int cnt = 0;
        for (int i = from; i < out.size(); i++) {
            if (hitsRuleLabels(out.get(i), ruleLabels)) {
                cnt++;
            }
        }
        cnt++;
        return cnt > maxInWindow;
    }

    private SlidingWindowRule loadRule() {
        String json = SpringUtils.getProperty(
                RecommendConstant.PROP_RERANK_SLIDING_WINDOW, RecommendConstant.DEFAULT_RERANK_SLIDING_WINDOW);
        if (CharSequenceUtil.isBlank(json)) {
            return null;
        }
        try {
            return JsonUtils.toObject(json, SlidingWindowRule.class);
        } catch (Exception e) {
            log.warn("RerankSlidingWindowDiversifyService#loadRule, parse fail, json={}", json, e);
            return null;
        }
    }
}
