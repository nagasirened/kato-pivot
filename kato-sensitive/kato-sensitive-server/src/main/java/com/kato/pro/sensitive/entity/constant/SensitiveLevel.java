package com.kato.pro.sensitive.entity.constant;

/**
 * 敏感词等级枚举
 */
public enum SensitiveLevel {

    URGENT("URGENT", "紧急"),
    MEDIUM("MEDIUM", "中等"),
    NORMAL("NORMAL", "普通");

    private final String code;
    private final String desc;

    SensitiveLevel(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public String getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    public static SensitiveLevel fromCode(String code) {
        for (SensitiveLevel level : values()) {
            if (level.code.equals(code)) {
                return level;
            }
        }
        return null;
    }
}
