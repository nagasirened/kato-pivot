package com.kato.pro.common.constant;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class BaseConstant {

    public static final String DEVICE_ID = "deviceId";
    public static final String USER_ID = "userId";
    public static final String TRACE_ID = "traceId";
    public static final String SUCCESS = "ok";
    public static final String ON = "on";
    public static final String OFF = "off";
    public static final String JUDGE_YES = "1";
    public static final String JUDGE_NO = "0";
    public static final String EMPTY_BRACKET = "[]";
    public static final String LOCK_PREFIX = "locker_";
    public static final String TOKEN_HEADER = "Authorization";
    public static final String BEARER_TYPE = "Bearer";
    public static final String MONTH_FORMAT = "yyyy-MM";
    public static final String DATE_FORMAT = "yyyy-MM-dd";
    public static final String DATETIME_FORMAT = "yyyy-MM-dd HH:mm:ss";
    public static final String SIMPLE_MONTH_FORMAT = "yyyyMM";
    public static final String TIME_ZONE_GMT8 = "GMT+8";
    public static final String DATE_FORMAT_SIMPLE = "MMdd";
    public static final String DATE_FORMAT_YMD = "yyyyMMdd";
    public static final String RECOMMEND_API_RATE_LIMIT = "api.rate.limit";

}
