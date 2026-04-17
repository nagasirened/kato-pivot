package com.kato.pro.sensitive.entity.constant;

/**
 * 敏感词状态枚举
 */
public enum SensitiveStatus {

    ENABLE("ENABLE", "启用"),
    DISABLE("DISABLE", "禁用");

    private final String code;
    private final String desc;

    SensitiveStatus(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public String getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    public static SensitiveStatus fromCode(String code) {
        for (SensitiveStatus status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        return ENABLE;
    }
}
