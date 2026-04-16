package com.kato.pro.rec.service.rerank;

import cn.hutool.core.collection.CollUtil;
import com.kato.pro.rec.entity.core.RecommendItem;
import com.kato.pro.rec.entity.po.RecommendParams;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 重排序流水线（Pipeline），串联多个重排序步骤：
 * <ol>
 *   <li>补全 label（{@link RerankLabelEnrichService}）：为每个商品写入召回源对应的 label</li>
 *   <li>label 加权（{@link RerankLabelWeightService}）：对命中配置 label 的商品做分数乘法加权</li>
 *   <li>按分数排序（降序），相同分数时按 itemId 升序排列</li>
 *   <li>去重（{@link #dedupePreserveOrder}）：同 itemId 只保留分数最高的那条</li>
 *   <li>强插（{@link RerankStrongInsertService}）：将命中规则 label 的商品固定到指定位置</li>
 *   <li>滑动窗口打散（{@link RerankSlidingWindowDiversifyService}）：保证任意连续 windowSize 个结果中，
 *       命中 label 的商品数不超过 maxInWindow（且不少于 minInWindow）</li>
 * </ol>
 *
 * @see RerankLabelEnrichService
 * @see RerankLabelWeightService
 * @see RerankStrongInsertService
 * @see RerankSlidingWindowDiversifyService
 */
@Service
public class RerankPipelineService {

    @Resource
    private RerankLabelEnrichService rerankLabelEnrichService;
    @Resource
    private RerankLabelWeightService rerankLabelWeightService;
    @Resource
    private RerankStrongInsertService rerankStrongInsertService;
    @Resource
    private RerankSlidingWindowDiversifyService rerankSlidingWindowDiversifyService;

    /**
     * 执行完整重排序流程。
     *
     * @param retrieveItems 召回阶段返回的原始商品列表（可能包含重复 itemId、分数未加权、无 label 信息）
     * @param params         重排序参数，核心字段为 topK（最多返回商品数）
     * @return 重排并截断后的商品列表，长度不超过 topK
     */
    public List<RecommendItem> rerank(List<RecommendItem> retrieveItems, RecommendParams params) {
        // 防御：空输入直接返回空列表
        if (CollUtil.isEmpty(retrieveItems)) {
            return new ArrayList<>();
        }
        // topK 默认 10，防止外部未传或传 null
        int topK = params.getTopK() == null ? 10 : params.getTopK();
        // 使用新列表避免修改原始传入列表
        List<RecommendItem> work = new ArrayList<>(retrieveItems);

        // Step 1: 补全 label——为每个商品写入其召回源对应的 label
        rerankLabelEnrichService.enrich(work);
        // Step 2: label 加权——对命中配置 label 的商品做分数乘法提升
        rerankLabelWeightService.applyWeight(work);

        // Step 3: 按分数降序排序，null 分数视作 0D；分数相同则按 itemId 升序（null 排最前）
        work.sort(
                Comparator.comparing((RecommendItem i) -> i.getScore() == null ? 0D : i.getScore())
                        .reversed()
                        .thenComparing(
                                i -> i.getItemId() == null ? Integer.MIN_VALUE : i.getItemId(),
                                Comparator.nullsFirst(Integer::compareTo)));

        // Step 4: 去重——同 itemId 只保留分数最高（排序后最先出现）的那条；无 itemId 的项顺排在后
        List<RecommendItem> deduped = dedupePreserveOrder(work);
        // Step 5: 强插——将命中规则 label 的商品固定到指定 0-based 位置
        List<RecommendItem> afterStrong = rerankStrongInsertService.apply(deduped, topK);
        // Step 6: 滑动窗口打散——保证任意连续 windowSize 个结果中，命中 label 商品数不超过 maxInWindow
        return rerankSlidingWindowDiversifyService.diversify(afterStrong, topK);
    }

    /**
     * 去重：同 itemId 保留分数排序下最先出现的一条；无 itemId 的项顺排在后，不去重。
     *
     * @param sorted 已按分数降序排列的商品列表（无重复 itemId 的输入可提升效率）
     * @return 去重后的商品列表，保留顺序为：去重后有 id 的商品 → 无 id 的商品
     */
    private static List<RecommendItem> dedupePreserveOrder(List<RecommendItem> sorted) {
        // byId 使用 LinkedHashMap 保证插入顺序，且 putIfAbsent 只保留第一条
        Map<Integer, RecommendItem> byId = new LinkedHashMap<>();
        // 无 id 的项无法参与去重，单独收集
        List<RecommendItem> noId = new ArrayList<>();
        for (RecommendItem it : sorted) {
            if (it.getItemId() == null) {
                noId.add(it);
            } else {
                // 只保留同 id 的第一条（排序后分数最高）
                byId.putIfAbsent(it.getItemId(), it);
            }
        }
        // 合并结果：先去重商品（按原排序），再接无 id 商品
        List<RecommendItem> out = new ArrayList<>(byId.size() + noId.size());
        out.addAll(byId.values());
        out.addAll(noId);
        return out;
    }
}
