package com.kato.pro.sensitive.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Set;

/**
 * 敏感词检测结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SensitiveCheckResult {

    /**
     * 是否包含敏感词
     */
    private boolean hasSensitive;

    /**
     * 匹配到的敏感词数量
     */
    private int matchCount;

    /**
     * 匹配结果列表
     */
    private List<MatchItem> matches;

    /**
     * 命中的敏感词分类集合
     */
    private Set<String> categories;

    /**
     * 敏感等级：URGENT/MEDIUM/NORMAL/NONE
     */
    private String maxLevel;

    /**
     * 脱敏后的文本（当sanitize=true时返回）
     */
    private String sanitizedText;

    /**
     * 是否为降级结果
     */
    private boolean fallback;

    /**
     * 匹配项
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MatchItem {
        /**
         * 匹配到的敏感词
         */
        private String word;

        /**
         * 敏感等级
         */
        private String level;

        /**
         * 匹配类型：EXACT/REGEX/FUZZY
         */
        private String matchType;

        /**
         * 起始位置
         */
        private int startIndex;

        /**
         * 结束位置
         */
        private int endIndex;
    }
}
