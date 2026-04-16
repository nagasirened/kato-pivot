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
 * 重排序流水线：补全 label → label 加权 → 按分数排序 → 去重 → 强插 → 滑动窗口打散。
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

    public List<RecommendItem> rerank(List<RecommendItem> retrieveItems, RecommendParams params) {
        if (CollUtil.isEmpty(retrieveItems)) {
            return new ArrayList<>();
        }
        int topK = params.getTopK() == null ? 10 : params.getTopK();
        List<RecommendItem> work = new ArrayList<>(retrieveItems);

        rerankLabelEnrichService.enrich(work);
        rerankLabelWeightService.applyWeight(work);

        work.sort(
                Comparator.comparing((RecommendItem i) -> i.getScore() == null ? 0D : i.getScore())
                        .reversed()
                        .thenComparing(
                                i -> i.getItemId() == null ? Integer.MIN_VALUE : i.getItemId(),
                                Comparator.nullsFirst(Integer::compareTo)));

        List<RecommendItem> deduped = dedupePreserveOrder(work);
        List<RecommendItem> afterStrong = rerankStrongInsertService.apply(deduped, topK);
        return rerankSlidingWindowDiversifyService.diversify(afterStrong, topK);
    }

    /**
     * 同 itemId 保留分数排序下最先出现的一条；无 itemId 的项顺排在后，不去重。
     */
    private static List<RecommendItem> dedupePreserveOrder(List<RecommendItem> sorted) {
        Map<Integer, RecommendItem> byId = new LinkedHashMap<>();
        List<RecommendItem> noId = new ArrayList<>();
        for (RecommendItem it : sorted) {
            if (it.getItemId() == null) {
                noId.add(it);
            } else {
                byId.putIfAbsent(it.getItemId(), it);
            }
        }
        List<RecommendItem> out = new ArrayList<>(byId.size() + noId.size());
        out.addAll(byId.values());
        out.addAll(noId);
        return out;
    }
}
