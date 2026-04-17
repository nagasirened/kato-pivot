package com.kato.pro.sensitive.entity.constant;

/**
 * 操作类型枚举
 */
public enum OperationType {

    CREATE("CREATE", "新增"),
    UPDATE("UPDATE", "修改"),
    DELETE("DELETE", "删除"),
    IMPORT("IMPORT", "批量导入"),
    EXPORT("EXPORT", "导出"),
    ENABLE("ENABLE", "启用"),
    DISABLE("DISABLE", "禁用");

    private final String code;
    private final String desc;

    OperationType(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public String getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }
}
