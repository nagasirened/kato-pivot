package com.kato.pro.rec.service;

import cn.hutool.core.collection.CollUtil;
import com.kato.pro.base.util.SpringUtils;
import com.kato.pro.rec.entity.constant.RecommendConstant;
import com.kato.pro.rec.entity.core.RecommendItem;
import com.kato.pro.rec.entity.core.RsInfo;
import com.kato.pro.rec.entity.po.RecommendParams;
import com.kato.pro.rec.service.retrieval.RetrieveStrategyHelper;
import io.micrometer.core.annotation.Timed;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
public class RetrievalService {

    @Resource private RetrieveCaptor retrieveCaptor;

    /**
     * 召回数据
     */
    @Timed(histogram = true, percentiles = {0.5, 0.9, 0.99})
    public List<RecommendItem> retrieve(RecommendParams request) {
        Map<String, String> abMap = request.getAbMap();
        // 获取所有的召回源, 并且过滤其中不符合条件的
        List<RsInfo> rsInfos = retrieveCaptor.wrapRecallSources(abMap);
        if (CollUtil.isEmpty(rsInfos)) {
            return new LinkedList<>();
        }
        // 开启多路召回并返回结果，key是召回源，value是召回结果
        Map<RsInfo, List<RecommendItem>> recallResultMap = RetrieveStrategyHelper.allOfAndReturn(rsInfos, request);
        // 多路结果按权重蛇形交织后截断为前 N 条
        return snakeSort(recallResultMap);
    }

    /**
     * 多路召回结果合并：可选「每路保底」+ 加权蛇形填剩余。
     * <p>阶段一（{@link RecommendConstant#PROP_SNAKE_MIN_PER_SOURCE} &gt; 0）：对每条非空召回路，按固定源顺序轮询，尽量各凑满
     * 配置条数的去重后有效商品，避免头部一路吃满 N 导致其它路零露出。
     * <p>阶段二：在剩余额度内做加权蛇形（每轮各路最多连续取 {@link RsInfo#getWeight()} 条），直到满 N 或无法再写入。
     * <p>配置键与默认值见 {@link RecommendConstant}，运行时通过 {@link SpringUtils} 读取（支持 Nacos 等动态刷新）。
     * <p>硬约束：若 N 小于非空召回路数且每路保底 ≥1，则物理上无法各路都占坑，会打 warn 日志。
     * 若某路候选与其它路 itemId 完全重复，该路仍可能占不到坑（无独立可去重项）。
     *
     * @param recallResultMap key 为召回源配置，value 为该路已排好序的候选列表（通常按召回侧分数从高到低）
     * @return 合并并截断后的列表，长度不超过 {@link RecommendConstant#PROP_SNAKE_MERGE_TOP_N} 的配置取值
     */
    private List<RecommendItem> snakeSort(Map<RsInfo, List<RecommendItem>> recallResultMap) {
        if (CollUtil.isEmpty(recallResultMap)) {
            return new ArrayList<>();
        }
        int mergeTopN = SpringUtils.getIntProperty(
                RecommendConstant.PROP_SNAKE_MERGE_TOP_N, RecommendConstant.DEFAULT_SNAKE_MERGE_TOP_N);
        if (mergeTopN <= 0) {
            return new ArrayList<>();
        }
        int minPerRaw = SpringUtils.getIntProperty(
                RecommendConstant.PROP_SNAKE_MIN_PER_SOURCE, RecommendConstant.DEFAULT_SNAKE_MIN_PER_SOURCE);
        int minPer = minPerRaw < 0 ? 0 : minPerRaw;
        SnakeMergeContext ctx = SnakeMergeContext.of(recallResultMap, mergeTopN, minPer);
        ctx.guaranteeMinPerSource();
        ctx.weightedSnakeFill();
        return ctx.getMerged();
    }

    /**
     * 一次蛇形合并的可变状态，避免在子步骤间传递长参数列表。
     */
    @Slf4j
    private static final class SnakeMergeContext {
        private final Map<RsInfo, List<RecommendItem>> recallResultMap;
        private final List<RsInfo> orderedSources;
        private final Map<RsInfo, Integer> cursorBySource;
        private final Set<Integer> seenItemIds;
        private final List<RecommendItem> merged;
        private final int mergeTopN;
        private final int minPerSource;

        private SnakeMergeContext(
                Map<RsInfo, List<RecommendItem>> recallResultMap,
                List<RsInfo> orderedSources,
                Map<RsInfo, Integer> cursorBySource,
                Set<Integer> seenItemIds,
                List<RecommendItem> merged,
                int mergeTopN,
                int minPerSource) {
            this.recallResultMap = recallResultMap;
            this.orderedSources = orderedSources;
            this.cursorBySource = cursorBySource;
            this.seenItemIds = seenItemIds;
            this.merged = merged;
            this.mergeTopN = mergeTopN;
            this.minPerSource = minPerSource;
        }

        static SnakeMergeContext of(Map<RsInfo, List<RecommendItem>> recallResultMap, int mergeTopN, int minPerSource) {
            // Map 遍历顺序不稳定，先固定召回源次序再合并
            List<RsInfo> orderedSources = recallResultMap.keySet().stream()
                    .sorted(Comparator.comparing(RsInfo::getLabel, Comparator.nullsLast(Integer::compareTo))
                            .thenComparing(rs -> Objects.toString(rs.getRsName(), "")))
                    .collect(Collectors.toList());
            Map<RsInfo, Integer> cursorBySource = new HashMap<>(orderedSources.size());
            for (RsInfo rs : orderedSources) {
                cursorBySource.put(rs, 0);
            }
            List<RecommendItem> merged = new ArrayList<>(Math.min(mergeTopN, 64));
            Set<Integer> seenItemIds = new HashSet<>();
            return new SnakeMergeContext(
                    recallResultMap, orderedSources, cursorBySource, seenItemIds, merged, mergeTopN, minPerSource);
        }

        List<RecommendItem> getMerged() {
            return merged;
        }

        /**
         * 阶段一：每路非空召回尽量先拿到 {@link #minPerSource} 条去重后的商品，再交给加权蛇形。
         */
        void guaranteeMinPerSource() {
            if (minPerSource <= 0) {
                return;
            }
            List<RsInfo> nonEmptyOrdered = orderedSources.stream()
                    .filter(rs -> !CollUtil.isEmpty(recallResultMap.get(rs)))
                    .collect(Collectors.toList());
            if (nonEmptyOrdered.isEmpty()) {
                return;
            }
            if (mergeTopN < nonEmptyOrdered.size() && minPerSource >= 1) {
                log.warn(
                        "RetrievalService#snakeSort, cannot satisfy minPerSource for every lane: snakeMergeTopN={} < nonEmptySourceCount={}, snakeMinPerSource={}",
                        mergeTopN,
                        nonEmptyOrdered.size(),
                        minPerSource);
            }
            Map<RsInfo, Integer> fulfilled = new HashMap<>(nonEmptyOrdered.size());
            for (RsInfo rs : nonEmptyOrdered) {
                fulfilled.put(rs, 0);
            }
            boolean roundProgress = true;
            while (merged.size() < mergeTopN && roundProgress) {
                roundProgress = false;
                for (RsInfo rsInfo : nonEmptyOrdered) {
                    if (merged.size() >= mergeTopN) {
                        break;
                    }
                    if (fulfilled.get(rsInfo) >= minPerSource) {
                        continue;
                    }
                    if (tryTakeNextDistinct(rsInfo)) {
                        fulfilled.merge(rsInfo, 1, Integer::sum);
                        roundProgress = true;
                    }
                }
                if (!nonEmptyOrdered.stream().anyMatch(rs -> fulfilled.get(rs) < minPerSource)) {
                    break;
                }
            }
        }

        /**
         * 阶段二：加权蛇形；progressed 表示上一轮是否至少写入过一条。
         */
        void weightedSnakeFill() {
            boolean progressed = true;
            while (merged.size() < mergeTopN && progressed) {
                progressed = false;
                for (RsInfo rsInfo : orderedSources) {
                    int weight = rsInfo.getWeight() == null ? 1 : rsInfo.getWeight();
                    if (weight < 1) {
                        weight = 1;
                    }
                    List<RecommendItem> lane = recallResultMap.get(rsInfo);
                    if (CollUtil.isEmpty(lane)) {
                        continue;
                    }
                    int idx = cursorBySource.get(rsInfo);
                    int taken = 0;
                    while (taken < weight && idx < lane.size() && merged.size() < mergeTopN) {
                        RecommendItem item = lane.get(idx++);
                        Integer itemId = item.getItemId();
                        if (itemId != null) {
                            if (!seenItemIds.add(itemId)) {
                                continue;
                            }
                        }
                        merged.add(item);
                        taken++;
                        progressed = true;
                    }
                    cursorBySource.put(rsInfo, idx);
                }
            }
        }

        /**
         * 从指定召回路游标起向后找第一条可并入 merged 的项（itemId 去重），成功则追加并推进游标。
         */
        boolean tryTakeNextDistinct(RsInfo rsInfo) {
            if (merged.size() >= mergeTopN) {
                return false;
            }
            List<RecommendItem> lane = recallResultMap.get(rsInfo);
            if (CollUtil.isEmpty(lane)) {
                return false;
            }
            int idx = cursorBySource.get(rsInfo);
            while (idx < lane.size()) {
                RecommendItem item = lane.get(idx++);
                Integer itemId = item.getItemId();
                if (itemId != null && !seenItemIds.add(itemId)) {
                    continue;
                }
                merged.add(item);
                cursorBySource.put(rsInfo, idx);
                return true;
            }
            cursorBySource.put(rsInfo, idx);
            return false;
        }
    }

}
