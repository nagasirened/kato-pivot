package com.kato.pro.sensitive.service;

import com.kato.pro.sensitive.dto.SensitiveCheckResult;

import java.util.List;

/**
 * 敏感词脱敏服务接口
 */
public interface SensitiveSanitizer {

    /**
     * 替换敏感词为指定字符
     *
     * @param text   原始文本
     * @param matches 匹配结果
     * @param maskChar 替换字符
     * @return 脱敏后的文本
     */
    String sanitize(String text, List<SensitiveCheckResult.MatchItem> matches, char maskChar);

    /**
     * 按等级替换
     * 高等级(URGENT)全替换，低等级(NORMAL)部分替换
     *
     * @param text      原始文本
     * @param matches   匹配结果
     * @param minLevel  最低替换等级，低于此等级的不替换
     * @param maskChar  替换字符
     * @return 脱敏后的文本
     */
    String sanitizeByLevel(String text, List<SensitiveCheckResult.MatchItem> matches, String minLevel, char maskChar);
}
