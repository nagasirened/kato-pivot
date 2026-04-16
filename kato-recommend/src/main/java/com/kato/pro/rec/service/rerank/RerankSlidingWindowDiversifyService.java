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
 * 滑动窗口打散：在保持 multiset 不变（即不丢弃商品）的前提下重排输出序列，
 * 使任意长度为 W（{@link SlidingWindowRule#getWindowSize()}）的连续子序列中，
 * 与规则 label 集合有交集的商品数不超过 {@link SlidingWindowRule#getMaxInWindow()}；
 * 若配置了 {@link SlidingWindowRule#getMinInWindow()}，则同时保证窗口内至少有该数量的匹配商品。
 * <p>无法在满足约束的前提下找到候选商品时，取候选队列队首（直接放宽约束），并记录
 * 类型为 {@link RerankRuleTypes#SLIDING_RELAX} 的轨迹。
 *
 * @see SlidingWindowRule
 * @see RerankRuleTypes#SLIDING_WINDOW
 * @see RerankRuleTypes#SLIDING_RELAX
 */
@Slf4j
@Service
public class RerankSlidingWindowDiversifyService {

    /**
     * 对已排序、去重、强插后的商品列表执行滑动窗口打散。
     *
     * @param ordered 输入商品列表（来自强插之后），按分数降序
     * @param topK    最多输出商品数
     * @return 打散后的商品列表，长度不超过 topK
     */
    public List<RecommendItem> diversify(List<RecommendItem> ordered, int topK) {
        if (CollUtil.isEmpty(ordered)) {
            return new ArrayList<>();
        }
        // 从配置加载滑动窗口规则
        SlidingWindowRule rule = loadRule();
        // 规则为空或未激活时，仅做截断返回
        if (rule == null || !isActive(rule)) {
            return trim(ordered, topK);
        }

        Set<Integer> ruleLabels = rule.effectiveLabels();
        // minInWindow 是否激活（配置了有效值才激活）
        boolean minActive = isMinActive(rule);
        // 记录打散前的顺序，用于后续标记位置变化
        List<RecommendItem> before = new ArrayList<>(ordered);
        // 候选池：可被选择的商品集合，随选取逐步移除
        List<RecommendItem> candidates = new ArrayList<>(ordered);
        List<RecommendItem> out = new ArrayList<>(Math.min(ordered.size(), topK));

        // 贪心选取：每次从候选池选出一个商品加入输出，直到满 topK 或候选池空
        while (!candidates.isEmpty() && out.size() < topK) {
            RecommendItem pick = null;
            int pickIndex = -1;

            // 分支一：minInWindow 已激活且当前输出末尾窗口尚未达到下限时，
            // 优先从候选池中找一个命中 label 且不会违反 max 约束的商品
            if (minActive && belowMin(out, ruleLabels, rule.getWindowSize(), rule.getMinInWindow())) {
                for (int i = 0; i < candidates.size(); i++) {
                    RecommendItem c = candidates.get(i);
                    // 必须同时满足：不超 max 约束 且 命中规则 label
                    if (!wouldViolate(out, c, ruleLabels, rule.getWindowSize(), rule.getMaxInWindow())
                            && hitsRuleLabels(c, ruleLabels)) {
                        pick = c;
                        pickIndex = i;
                        break;
                    }
                }
            }

            // 分支二：未找到满足 min 的商品时，退化为只满足 max 约束的贪心选取
            // 选候选池中第一个加入后不会超 max 的商品（尽可能保持原顺序）
            if (pick == null) {
                for (int i = 0; i < candidates.size(); i++) {
                    RecommendItem c = candidates.get(i);
                    if (!wouldViolate(out, c, ruleLabels, rule.getWindowSize(), rule.getMaxInWindow())) {
                        pick = c;
                        pickIndex = i;
                        break;
                    }
                }
            }

            // 分支三（放宽约束）：若 max/min 约束在候选池中完全无法满足，
            // 强制取候选池队首，并在 rerankTrace 中记录 SLIDING_RELAX
            if (pick == null) {
                pick = candidates.get(0);
                pickIndex = 0;
                log.warn(
                        "RerankSlidingWindowDiversifyService#diversify, relax constraint, ruleLabels={}, windowSize={}",
                        ruleLabels,
                        rule.getWindowSize());
                pick.appendRerankTrace(RerankRuleTypes.SLIDING_RELAX, new HashSet<>(ruleLabels), out.size());
            }
            // 从候选池移除已选商品，加入输出
            candidates.remove(pickIndex);
            out.add(pick);
        }

        // 标记相对打散前发生位置变化的商品（用于链路追踪）
        tagSlidingRepositions(before, out, ruleLabels);
        return out;
    }

    /**
     * 标记相对打散前序列发生位置变化的商品，追加 {@link RerankRuleTypes#SLIDING_WINDOW} 轨迹。
     * 仅关注命中 ruleLabels 的商品（因为只有这些商品的位置才有打散意义）。
     *
     * @param before     打散前的商品列表（完整顺序）
     * @param after      打散后的商品列表（输出顺序）
     * @param ruleLabels 规则关注的 label 集合（用于过滤；传空则直接跳过）
     */
    private static void tagSlidingRepositions(List<RecommendItem> before, List<RecommendItem> after, Set<Integer> ruleLabels) {
        if (CollUtil.isEmpty(ruleLabels)) {
            return;
        }
        // 构建 id -> 打散前下标的映射，用于快速比较
        Map<Integer, Integer> idToIndexBefore = new HashMap<>();
        for (int i = 0; i < before.size(); i++) {
            RecommendItem it = before.get(i);
            if (it.getItemId() != null) {
                idToIndexBefore.put(it.getItemId(), i);
            }
        }
        Set<Integer> labelSnapshot = new HashSet<>(ruleLabels);
        // 遍历打散后序列：若商品命中 ruleLabels 且打散后下标与原下标不同，则标记 SLIDING_WINDOW
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

    /**
     * 对商品列表做 topK 截断，无任何规则时降级使用。
     */
    private static List<RecommendItem> trim(List<RecommendItem> ordered, int topK) {
        int n = Math.min(topK, ordered.size());
        return new ArrayList<>(ordered.subList(0, n));
    }

    /**
     * 判断滑动窗口规则是否激活：windowSize > 0、maxInWindow >= 0 且 effectiveLabels 非空。
     */
    private static boolean isActive(SlidingWindowRule rule) {
        return rule.getWindowSize() != null
                && rule.getWindowSize() > 0
                && CollUtil.isNotEmpty(rule.effectiveLabels())
                && rule.getMaxInWindow() != null
                && rule.getMaxInWindow() >= 0;
    }

    /**
     * 判断 minInWindow 约束是否激活：minInWindow >= 0（配置了才激活）。
     */
    private static boolean isMinActive(SlidingWindowRule rule) {
        return rule.getMinInWindow() != null && rule.getMinInWindow() >= 0;
    }

    /**
     * 判断商品 labels 与规则 label 集合是否有交集（商品命中规则）。
     */
    private static boolean hitsRuleLabels(RecommendItem it, Set<Integer> ruleLabels) {
        return it.getLabels() != null && !Collections.disjoint(it.getLabels(), ruleLabels);
    }

    /**
     * 判断将候选商品加入当前输出末尾后，末尾 windowSize 窗口内命中 ruleLabels 的商品数是否会超过 maxInWindow。
     * <p>计算逻辑：取当前输出末尾 length = windowSize 的子序列（不足则全取），
     * 统计其中已命中 label 的商品数，再加上候选商品本身（若命中），判断是否超限。
     *
     * @param out          当前已输出的商品列表
     * @param cand         候选商品
     * @param ruleLabels   规则 label 集合
     * @param windowSize   滑动窗口大小
     * @param maxInWindow  窗口内最多允许的命中商品数
     * @return true 表示会违反约束（不应选取）；false 表示不会违反
     */
    private static boolean wouldViolate(
            List<RecommendItem> out,
            RecommendItem cand,
            Set<Integer> ruleLabels,
            int windowSize,
            int maxInWindow) {
        // 不命中 label 的商品天然不产生约束冲突
        if (!hitsRuleLabels(cand, ruleLabels)) {
            return false;
        }
        // 计算窗口起始下标：从末尾往前取 windowSize 个
        int from = Math.max(0, out.size() - (windowSize - 1));
        int cnt = 0;
        for (int i = from; i < out.size(); i++) {
            if (hitsRuleLabels(out.get(i), ruleLabels)) {
                cnt++;
            }
        }
        // 候选商品自身若命中 label，也需要算入
        cnt++;
        return cnt > maxInWindow;
    }

    /**
     * 判断当前输出末尾窗口内命中的 label 商品数是否已达到 minInWindow 下限。
     *
     * @param out          当前已输出的商品列表
     * @param ruleLabels   规则 label 集合
     * @param windowSize   滑动窗口大小
     * @param minInWindow  窗口内最少需要的命中商品数（须 >= 0 才激活）
     * @return true 表示尚未达到下限，需要优先选命中 label 的商品
     */
    private static boolean belowMin(
            List<RecommendItem> out,
            Set<Integer> ruleLabels,
            int windowSize,
            int minInWindow) {
        if (minInWindow <= 0) {
            return false;
        }
        int from = Math.max(0, out.size() - (windowSize - 1));
        int cnt = 0;
        for (int i = from; i < out.size(); i++) {
            if (hitsRuleLabels(out.get(i), ruleLabels)) {
                cnt++;
            }
        }
        return cnt < minInWindow;
    }

    /**
     * 从 Spring 配置加载滑动窗口规则。
     *
     * @return 解析后的规则对象，配置为空或解析失败时返回 null（流水线将降级为透传截断）
     */
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
