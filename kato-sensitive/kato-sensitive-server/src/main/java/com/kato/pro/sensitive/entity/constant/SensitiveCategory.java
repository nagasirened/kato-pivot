package com.kato.pro.sensitive.entity.constant;

/**
 * 敏感词分类枚举
 */
public enum SensitiveCategory {

    POLITICS("POLITICS", "政治"),
    PORN("PORN", "色情"),
    AD("AD", "广告"),
    VIOLENCE("VIOLENCE", "暴恐"),
    FRAUD("FRAUD", "诈骗"),
    OTHER("OTHER", "其他");

    private final String code;
    private final String desc;

    SensitiveCategory(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public String getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    public static SensitiveCategory fromCode(String code) {
        for (SensitiveCategory category : values()) {
            if (category.code.equals(code)) {
                return category;
            }
        }
        return OTHER;
    }

    public static SensitiveCategory[] getAllCategories() {
        return values();
    }
}
