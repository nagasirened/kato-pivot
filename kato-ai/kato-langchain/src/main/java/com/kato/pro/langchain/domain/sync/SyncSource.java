package com.kato.pro.langchain.domain.sync;

import com.kato.pro.langchain.domain.knowledge.SourceType;

/**
 * 同步源（adapter 标识 → KnowledgeDoc.SourceType 映射）。
 *
 * v1 仅 ProductCatalogAdapter；POLICY/FAQ 留 TODO 后续添加。
 */
public enum SyncSource {
    PRODUCT("product_catalog", SourceType.SYNC_PRODUCT, "商品库"),
    POLICY("policy_doc", SourceType.SYNC_POLICY, "政策库"),
    FAQ("faq", SourceType.SYNC_FAQ, "FAQ 库");

    private final String adapterName;
    private final SourceType sourceType;
    private final String description;

    SyncSource(String adapterName, SourceType sourceType, String description) {
        this.adapterName = adapterName;
        this.sourceType = sourceType;
        this.description = description;
    }

    public String adapterName() { return adapterName; }
    public SourceType sourceType() { return sourceType; }
    public String description() { return description; }

    public static SyncSource fromAdapterName(String name) {
        for (SyncSource s : values()) {
            if (s.adapterName.equalsIgnoreCase(name)) return s;
        }
        return null;
    }
}
