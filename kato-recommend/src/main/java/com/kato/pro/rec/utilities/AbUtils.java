package com.kato.pro.rec.utilities;

public class AbUtils {

    /**
     * 默认的AB检测，结果映射 "1"->True  "0"->False
     */
    public static boolean baseCheck(String val) {
        return "1".equals(val);
    }

}
