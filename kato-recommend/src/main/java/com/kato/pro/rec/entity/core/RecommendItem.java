package com.kato.pro.rec.entity.core;

import lombok.Data;

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
