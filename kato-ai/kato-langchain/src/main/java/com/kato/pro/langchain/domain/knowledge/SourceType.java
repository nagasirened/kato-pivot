package com.kato.pro.langchain.domain.knowledge;

import com.baomidou.mybatisplus.annotation.EnumValue;

/**
 * 知识库文档来源类型。
 *
 *   UPLOAD       — 管理员手动上传
 *   SYNC_PRODUCT — 商品库同步（M9）
 *   SYNC_POLICY  — 政策库同步（M9）
 *   SYNC_FAQ     — FAQ 同步（M9）
 */
public enum SourceType {
    UPLOAD("UPLOAD"),
    SYNC_PRODUCT("SYNC_PRODUCT"),
    SYNC_POLICY("SYNC_POLICY"),
    SYNC_FAQ("SYNC_FAQ");

    @EnumValue
    private final String code;

    SourceType(String code) { this.code = code; }

    public String code() { return code; }

    public static SourceType fromCode(String code) {
        if (code == null) return UPLOAD;
        for (SourceType t : values()) if (t.code.equals(code)) return t;
        throw new IllegalArgumentException("Unknown SourceType code: " + code);
    }
}
