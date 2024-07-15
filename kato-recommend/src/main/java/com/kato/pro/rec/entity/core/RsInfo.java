package com.kato.pro.rec.entity.core;

import com.kato.pro.rec.entity.enums.RsEnum;
import lombok.*;

import java.util.Arrays;


/**
 * 召回源配置
 */
@Getter
@Setter
@EqualsAndHashCode(of = {"rsName"})
public class RsInfo {
    private Integer label;
    /* 召回源名称 */
    private String rsName;
    /* 召回上限 */
    private Integer rsLimit;
    /* 权重 */
    private Integer weight;
    /* 用户new|old，不填代表不用过滤 */
    private Boolean accessible;
    /* 唯一召回源过滤 */
    private Boolean unique;
    /* 拓展参数 */
    private String spare;
    /* 限制用户类别 old\new\... */
    private String userType;


    public RsInfo(String rsName) {
        this(rsName, 1);
    }

    public RsInfo(String rsName, Integer weight) {
        this(rsName, weight, 100);
    }

    public RsInfo(String rsName, Integer weight, Integer rsLimit) {
        this.rsName = rsName;
        this.weight = weight;
        this.rsLimit = rsLimit;
    }

    public void init() {
        if (this.rsLimit == null) {
            this.rsLimit = 100;
        }
        if (this.weight == null) {
            this.weight = 1;
        }
    }

}
