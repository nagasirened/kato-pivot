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
 */
@Service
public class RerankLabelEnrichService {

    public void enrich(List<RecommendItem> items) {
        if (CollUtil.isEmpty(items)) {
            return;
        }
        for (RecommendItem item : items) {
            Set<Integer> labels = item.getLabels();
            if (labels == null) {
                labels = new HashSet<>();
                item.setLabels(labels);
            }
            Integer recallLabel = RsEnum.getLabelByCode(item.getRs());
            if (recallLabel != null) {
                labels.add(recallLabel);
            }
        }
    }
}
