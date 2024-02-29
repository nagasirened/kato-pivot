package com.kato.pro.base.util;

import cn.hutool.core.date.DateUtil;
import com.kato.pro.base.constant.CommonConstant;
import lombok.experimental.UtilityClass;

import java.util.Date;

@UtilityClass
public class DateHelper {

    /**
     * MMdd
     */
    public String currentMMdd() {
        return DateUtil.format(new Date(), CommonConstant.DATE_FORMAT_SIMPLE);
    }

    /**
     * yyyyMMdd
     */
    public String currentDateStr() {
        return DateUtil.format(new Date(), CommonConstant.DATE_FORMAT_YMD);
    }

}
