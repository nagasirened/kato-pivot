package com.kato.pro.rec.entity.core;

import com.kato.pro.rec.entity.dto.RerankTraceEntry;
import lombok.Data;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Data
public class RecommendItem {

    private Integer itemId;
    private Double score = 0D;
    private Double rankScore;
    private String rs;
    private Set<Integer> labels;
    private Map<String, Set<String>> clusters;
    /** 因强插、打散等规则被摆到当前位时的轨迹，按发生顺序追加 */
    private List<RerankTraceEntry> rerankTrace;

    /**
     * 追加一条重排轨迹（强插 / 打散 / 打散放宽等）。
     *
     * @param ruleType   规则类型常量（如 RerankRuleTypes.STRONG_INSERT / SLIDING_WINDOW / SLIDING_RELAX）
     * @param ruleLabels 规则配置的 label 集合快照，可为 null
     * @param position   写入输出列表时的 0-based 下标，无则传 null
     */
    public void appendRerankTrace(String ruleType, Set<Integer> ruleLabels, Integer position) {
        if (rerankTrace == null) {
            rerankTrace = new ArrayList<>();
        }
        RerankTraceEntry entry = new RerankTraceEntry();
        entry.setRuleType(ruleType);
        entry.setRuleLabels(ruleLabels == null ? null : new HashSet<>(ruleLabels));
        entry.setPosition(position);
        rerankTrace.add(entry);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RecommendItem item = (RecommendItem) o;
        return Objects.equals(item.getItemId(), itemId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(itemId);
    }
}
