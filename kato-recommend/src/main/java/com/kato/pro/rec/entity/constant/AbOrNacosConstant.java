package com.kato.pro.rec.entity.constant;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class AbOrNacosConstant {

    // 浏览曝光的天数
    public static final String RECOMMEND_IMPRESSION_DAYS = "rec.impression.days";
    // 最近播放的数量
    public static final String RECOMMEND_PLAYED_NUMBER = "rec.readings.number";


    /** whether filter black-list for impression-model */
    public static final String REC_FILTER_BLACK_SWITCH = "rec_filter_black_switch";
    public static final String REC_FILTER_BLACK_LIST = "rec_filter_black_list";
    public static final String OBTAIN_NUMBER = "retrieve.obtain_number";

}
