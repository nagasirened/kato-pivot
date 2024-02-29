package com.kato.pro.base.util;

import com.kato.pro.base.service.NacosPropertyAcquirer;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

@Slf4j
public class PropertyUtil {

    public static final String SUCCESS = "1";

    public static boolean verifySwitch(NacosPropertyAcquirer acquirer, Map<String, String> abMap, String key, String defaultValue) {
        String property = acquirer.getAbOrProperty(abMap, key, defaultValue);
        return SUCCESS.equals(property);
    }

}
