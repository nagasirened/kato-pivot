package com.kato.pro.rec.service.rerank;

import cn.hutool.core.collection.CollUtil;
import com.kato.pro.rec.entity.core.RecommendItem;
import com.kato.pro.rec.entity.enums.RsEnum;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 为重排序准备 labels：当前阶段仅写入召回源对应 label（{@link RsEnum#getLabelByCode}）。
 * <p>重排序流程中 label 用于强插规则匹配和滑动窗口打散约束判断，
 * 因此在进入重排序流水线前须先调用本服务补全 label 信息。
 *
 * @see RerankStrongInsertService
 * @see RerankSlidingWindowDiversifyService
 */
@Service
public class RerankLabelEnrichService {

    /**
     * 为每个商品补全 label 信息，将商品召回源对应的 label 写入其 labels 集合。
     *
     * @param items 待处理的商品列表（会直接修改 item.getLabels()）
     */
    public void enrich(List<RecommendItem> items) {
        if (CollUtil.isEmpty(items)) {
            return;
        }
        for (RecommendItem item : items) {
            // 确保 labels 集合不为 null，避免后续集合操作 NPE
            Set<Integer> labels = item.getLabels();
            if (labels == null) {
                labels = new HashSet<>();
                item.setLabels(labels);
            }
            // 根据商品召回源（rs 字段）获取对应的 label 并写入集合
            Integer recallLabel = RsEnum.getLabelByCode(item.getRs());
            if (recallLabel != null) {
                labels.add(recallLabel);
            }
        }
    }
}
