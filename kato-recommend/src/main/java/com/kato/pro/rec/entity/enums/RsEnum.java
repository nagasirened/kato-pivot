package com.kato.pro.rec.entity.enums;

import lombok.Getter;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Getter
public enum RsEnum {

    DEFAULT("default_v1", 100, true),
    ;

    public final String rsName;
    public final Integer label;
    public final Boolean enable;

    RsEnum(String rsName, int label, boolean enable) {
        this.rsName = rsName;
        this.label = label;
        this.enable = enable;
    }

    static final Map<String, Integer> rsNameLabelMap;
    static {
        rsNameLabelMap = Stream.of(RsEnum.values()).collect(Collectors.toMap(RsEnum::getRsName, RsEnum::getLabel));
    }

    public static Integer getLabelByCode(String rsName) {
        return rsNameLabelMap.get(rsName);
    }

}
