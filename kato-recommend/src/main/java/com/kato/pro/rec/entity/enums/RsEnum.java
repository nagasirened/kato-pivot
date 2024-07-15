package com.kato.pro.rec.entity.enums;

import com.kato.pro.base.util.ConfigUtils;
import com.kato.pro.rec.entity.core.RecommendItem;
import com.kato.pro.rec.entity.core.RetrieveStrategy;
import com.kato.pro.rec.entity.core.RsInfo;
import com.kato.pro.rec.entity.po.RecommendParams;
import lombok.Getter;

import java.util.List;
import java.util.Map;

@Getter
public enum RsEnum {

    DEFAULT("default_v1", 100, true),
    ;

    public final String code;
    public final Integer label;
    public final Boolean enable;

    RsEnum(String code, int label, boolean enable) {
        this.code = code;
        this.label = label;
        this.enable = enable;
    }

    static final Map<String, RetrieveStrategy> retrieveBeanMap;
    static {
        retrieveBeanMap = ConfigUtils.getBeanMapByType(RetrieveStrategy.class);
    }

    public List<RecommendItem> recall(RsInfo rsInfo, RecommendParams params) {
        RetrieveStrategy retrieveStrategy = retrieveBeanMap.get(this.code);
        return retrieveStrategy.recall(rsInfo, params);
    }

}
