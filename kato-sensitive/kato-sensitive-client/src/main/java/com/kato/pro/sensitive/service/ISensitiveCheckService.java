package com.kato.pro.sensitive.service;

import com.kato.pro.sensitive.dto.SensitiveCheckResult;

/**
 * 敏感词检测服务接口
 */
public interface ISensitiveCheckService {

    /**
     * 检测文本敏感词
     */
    SensitiveCheckResult check(String text);

    /**
     * 检测文本敏感词（指定是否模糊匹配）
     */
    SensitiveCheckResult check(String text, boolean fuzzyMatch);

    /**
     * 按等级检测文本
     * 只返回大于等于指定等级的敏感词
     */
    SensitiveCheckResult checkByLevel(String text, String minLevel);

    /**
     * 精确匹配检测
     */
    SensitiveCheckResult checkExact(String text);

    /**
     * 模糊匹配检测（同音字、形近字等）
     */
    SensitiveCheckResult checkFuzzy(String text);
}
