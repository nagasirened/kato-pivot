package com.kato.pro.rec.entity.constant;

public interface AbOrNacosConstant {

    // 浏览曝光的天数
    String RECOMMEND_IMPRESSION_DAYS = "rec.impression.days";
    // 最近播放的数量
    String RECOMMEND_PLAYED_NUMBER = "rec.readings.number";


    /** whether filter black-list for impression-model */
    String REC_FILTER_BLACK_SWITCH = "rec_filter_black_switch";
    String REC_FILTER_BLACK_LIST = "rec_filter_black_list";
    String OBTAIN_NUMBER = "retrieve.obtain_number";

    /** 直接推荐高热内容 */
    String DIRECT_PUSH_HOT = "direct_hot";
    /** 用户召回源 */
    String USER_RECALL_SOURCE = "user_rs";
    String DEFAULT_NORMAL_RECALL_SOURCES = "default_normal_rs";
    String DEFAULT_SPECIAL_RECALL_SOURCES = "default_special_rs";

}
