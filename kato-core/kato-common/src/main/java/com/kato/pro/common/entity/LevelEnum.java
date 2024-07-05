package com.kato.pro.common.entity;

import lombok.Getter;

public enum LevelEnum {

    NORMAL(1),
    MIDDLE(2),
    DETAIL(3),
    ;

    @Getter
    private Integer level;

    LevelEnum(Integer level) {
        this.level = level;
    }
}