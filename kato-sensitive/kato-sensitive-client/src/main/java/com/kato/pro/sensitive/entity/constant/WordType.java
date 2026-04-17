package com.kato.pro.sensitive.entity.constant;

/**
 * 敏感词类型枚举
 */
public enum WordType {

    EXACT("EXACT", "精确匹配"),
    REGEX("REGEX", "正则匹配");

    private final String code;
    private final String desc;

    WordType(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public String getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    public static WordType fromCode(String code) {
        if (code == null) {
            return EXACT;
        }
        for (WordType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        return EXACT;
    }
}
