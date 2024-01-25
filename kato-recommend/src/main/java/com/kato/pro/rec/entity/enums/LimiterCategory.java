package com.kato.pro.rec.entity.enums;

public enum LimiterCategory {

    RECOMMEND,
    ;

    public String lowerName() {
        return this.name().toLowerCase();
    }

}
