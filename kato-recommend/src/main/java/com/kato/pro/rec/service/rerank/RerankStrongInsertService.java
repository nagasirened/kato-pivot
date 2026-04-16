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
 * 强插（Strong Insert）：在 topK 输出长度内，
 * 将「商品 labels 与规则 label 集合有交集」的商品固定到配置的 0-based 位置，
 * 其余位置按分数顺序从候选池中依序填充。
 *
 * <p>规则按 position 升序逐个执行；若某 position 已有商品（多规则冲突）或找不到匹配商品，则该规则静默跳过。
 * <p>每个被强插的商品会在 {@link RecommendItem#getRerankTrace()} 中记录类型为
 * {@link RerankRuleTypes#STRONG_INSERT} 的轨迹。
 *
 * @see RerankRuleTypes#STRONG_INSERT
 */
@Slf4j
@Service
public class RerankStrongInsertService {

    /**
     * 对已去重、已排序的商品池执行强插规则。
     *
     * @param sortedDedupedPool 已按分数降序排列、去重后的商品候选池
     * @param topK              最多输出商品数
     * @return 强插处理后的商品列表，长度不超过 topK
     */
    public List<RecommendItem> apply(List<RecommendItem> sortedDedupedPool, int topK) {
        // 防御：空输入或无效 topK 直接返回空列表
        if (topK <= 0 || CollUtil.isEmpty(sortedDedupedPool)) {
            return new ArrayList<>();
        }
        // 从配置加载强插规则列表
        List<StrongInsertRule> rules = loadRules();
        // 无规则时直接返回前 topK 条（透传）
        if (CollUtil.isEmpty(rules)) {
            return takeHead(sortedDedupedPool, topK);
        }
        // 按 position 升序排列，确保位置编号小的规则先执行
        rules.sort(Comparator.comparingInt(r -> r.getPosition() == null ? Integer.MAX_VALUE : r.getPosition()));

        // slots 数组：长度为 topK，下标即输出位置，先预留 null 表示未填充
        RecommendItem[] slots = new RecommendItem[topK];
        // 已放置商品的 id 集合，防止同一商品被多次放入不同 slot
        Set<Integer> placedIds = new HashSet<>();

        // 逐条规则执行强插
        for (StrongInsertRule rule : rules) {
            Set<Integer> ruleLabels = rule.effectiveLabels();
            // 跳过无有效 label 或无 position 的规则
            if (CollUtil.isEmpty(ruleLabels) || rule.getPosition() == null) {
                continue;
            }
            int pos = rule.getPosition();
            // position 超出有效范围或该位置已被占时跳过
            if (pos < 0 || pos >= topK) {
                log.warn("RerankStrongInsertService#apply, skip rule, invalid position={}, topK={}", pos, topK);
                continue;
            }
            if (slots[pos] != null) {
                // 多规则指向同位置，先到先得，后到跳过
                continue;
            }
            // 从候选池顺序查找第一个命中该规则 label 且未被放置的商品
            RecommendItem chosen = pickFirstMatchingLabels(sortedDedupedPool, ruleLabels, placedIds);
            if (chosen != null) {
                // 占据目标位置
                slots[pos] = chosen;
                // 记录强插轨迹：类型、固定该位置的 label 快照、目标位置
                chosen.appendRerankTrace(RerankRuleTypes.STRONG_INSERT, new HashSet<>(ruleLabels), pos);
                if (chosen.getItemId() != null) {
                    placedIds.add(chosen.getItemId());
                }
            }
        }

        // 填充 slots 中剩余的空位：从候选池顺序扫描，跳过已放置的 id，填入第一个可用商品
        int scan = 0;
        for (int p = 0; p < topK; p++) {
            if (slots[p] != null) {
                continue;
            }
            while (scan < sortedDedupedPool.size()) {
                RecommendItem it = sortedDedupedPool.get(scan++);
                Integer id = it.getItemId();
                // 跳过已强插占位的商品，避免重复
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

        // 将 slots 数组压缩为列表（跳过末尾可能的 null）
        List<RecommendItem> out = new ArrayList<>(topK);
        for (RecommendItem s : slots) {
            if (s != null) {
                out.add(s);
            }
        }
        return out;
    }

    /**
     * 取候选池前 topK 条商品，用于无规则时的直接透传。
     */
    private static List<RecommendItem> takeHead(List<RecommendItem> pool, int topK) {
        int n = Math.min(topK, pool.size());
        return new ArrayList<>(pool.subList(0, n));
    }

    /**
     * 判断商品 label 集合与规则 label 集合是否有交集（即商品是否命中该规则）。
     * 使用 {@link Collections#disjoint} 高效判断（内部为交集大小判断）。
     */
    private static boolean labelsIntersectRule(Set<Integer> itemLabels, Set<Integer> ruleLabels) {
        if (CollUtil.isEmpty(itemLabels) || CollUtil.isEmpty(ruleLabels)) {
            return false;
        }
        return !Collections.disjoint(itemLabels, ruleLabels);
    }

    /**
     * 从候选池顺序查找第一个同时满足以下条件的商品：
     * <ul>
     *   <li>商品 labels 与规则 label 集合有交集（命中规则）</li>
     *   <li>商品 id 不在已放置 id 集合中（未被强插占用）</li>
     * </ul>
     *
     * @param sortedPool 已按分数降序排列的候选池
     * @param ruleLabels 规则的有效 label 集合
     * @param placedIds  已被强插占用的商品 id 集合
     * @return 第一个匹配的商品，若无匹配则返回 null
     */
    private static RecommendItem pickFirstMatchingLabels(
            List<RecommendItem> sortedPool, Set<Integer> ruleLabels, Set<Integer> placedIds) {
        for (RecommendItem it : sortedPool) {
            Integer id = it.getItemId();
            // 跳过已被占用的商品
            if (id != null && placedIds.contains(id)) {
                continue;
            }
            if (labelsIntersectRule(it.getLabels(), ruleLabels)) {
                return it;
            }
        }
        return null;
    }

    /**
     * 从 Spring 配置加载强插规则列表。
     *
     * @return 规则列表，无配置或解析失败时返回空列表
     */
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
            // 解析失败时返回空列表，使流水线降级为跳过强插步骤（不阻塞）
            log.warn("RerankStrongInsertService#loadRules, parse fail, json={}", json, e);
            return new ArrayList<>();
        }
    }
}
