package com.kato.pro.sensitive.dto;

import lombok.Data;

/**
 * 敏感词检测请求
 */
@Data
public class SensitiveCheckRequest {

    /**
     * 待检测文本
     */
    private String text;

    /**
     * 是否启用模糊匹配（同音字、形近字等）
     */
    private boolean fuzzyMatch = false;

    /**
     * 检测模式：EXACT-精确, FUZZY-模糊, ALL-全部
     */
    private String mode = "ALL";

    /**
     * 是否返回脱敏文本
     */
    private boolean sanitize = false;

    /**
     * 脱敏替换字符，默认*
     */
    private char maskChar = '*';

    /**
     * 检测类型：EXACT-精确, REGEX-正则, ALL-全部
     */
    private String wordType = "ALL";
}
