package com.kato.pro.rec.entity.enums;

import com.kato.pro.base.util.ConfigUtils;
import com.kato.pro.rec.entity.core.RecommendItem;
import com.kato.pro.rec.entity.core.RetrieveStrategy;
import com.kato.pro.rec.entity.core.RsInfo;
import com.kato.pro.rec.entity.po.RecommendParams;

import java.util.List;
import java.util.Map;

public enum RsEnum {

    DEFAULT("default_v1", 100, true),
    ;

    public final String name;
    public final Integer label;
    public final boolean enable;

    RsEnum(String name, int label, boolean enable) {
        this.name = name;
        this.label = label;
        this.enable = enable;
    }

    final static Map<String, RetrieveStrategy> retrieveBeanMap;
    static {
        retrieveBeanMap = ConfigUtils.getBeanMapByType(RetrieveStrategy.class);
    }

    public List<RecommendItem> recall(RsInfo rsInfo, RecommendParams params) {
        RetrieveStrategy retrieveStrategy = retrieveBeanMap.get(this.name);
        return retrieveStrategy.recall(rsInfo, params);
    }

}
