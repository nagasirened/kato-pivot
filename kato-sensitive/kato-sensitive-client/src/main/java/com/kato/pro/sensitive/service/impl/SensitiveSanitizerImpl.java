package com.kato.pro.sensitive.service.impl;

import com.kato.pro.sensitive.dto.SensitiveCheckResult;
import com.kato.pro.sensitive.service.SensitiveSanitizer;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

/**
 * 敏感词脱敏服务实现
 */
@Service
public class SensitiveSanitizerImpl implements SensitiveSanitizer {

    private static final char DEFAULT_MASK_CHAR = '*';

    @Override
    public String sanitize(String text, List<SensitiveCheckResult.MatchItem> matches, char maskChar) {
        if (text == null || text.isEmpty() || matches == null || matches.isEmpty()) {
            return text;
        }

        // 按起始位置倒序排列，避免位置偏移问题
        List<SensitiveCheckResult.MatchItem> sortedMatches = matches.stream()
                .sorted(Comparator.comparingInt(SensitiveCheckResult.MatchItem::getStartIndex).reversed())
                .collect(java.util.stream.Collectors.toList());

        StringBuilder result = new StringBuilder(text);
        for (SensitiveCheckResult.MatchItem match : sortedMatches) {
            int start = match.getStartIndex();
            int end = match.getEndIndex();
            if (start >= 0 && end <= result.length() && start < end) {
                String mask = String.valueOf(maskChar).repeat(end - start);
                result.replace(start, end, mask);
            }
        }

        return result.toString();
    }

    @Override
    public String sanitizeByLevel(String text, List<SensitiveCheckResult.MatchItem> matches, String minLevel, char maskChar) {
        if (text == null || text.isEmpty() || matches == null || matches.isEmpty()) {
            return text;
        }

        int minLevelValue = getLevelValue(minLevel);

        // 过滤出需要替换的匹配项
        List<SensitiveCheckResult.MatchItem> matchesToSanitize = matches.stream()
                .filter(m -> getLevelValue(m.getLevel()) >= minLevelValue)
                .collect(java.util.stream.Collectors.toList());

        return sanitize(text, matchesToSanitize, maskChar);
    }

    private int getLevelValue(String level) {
        if (level == null) {
            return 0;
        }
        String upperLevel = level.toUpperCase();
        if ("URGENT".equals(upperLevel)) {
            return 3;
        } else if ("MEDIUM".equals(upperLevel)) {
            return 2;
        } else if ("NORMAL".equals(upperLevel)) {
            return 1;
        }
        return 0;
    }
}
