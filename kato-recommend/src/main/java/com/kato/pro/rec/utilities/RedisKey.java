package com.kato.pro.rec.utilities;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import lombok.Getter;

@Getter
public enum RedisKey {

    REC_CONTENT_IMPRESSION("rec_impression", 86400 * 3),
    REC_CONTENT_PLAYED("rec_played", 86400 * 10),
    RETRIEVE_DEVICE_OBTAIN_NUMBER("obtain_number", 86400 + 10),
    ;

    private final String prefix;
    private final Integer expire;

    RedisKey(String prefix, Integer expire) {
        this.prefix = prefix;
        this.expire = expire;
    }

    public String makeRedisKey(Object... details) {
        return assembleKey(":", details);
    }

    public String makeRedisKeyWithSeparator(String separator, Object... details) {
        return assembleKey(separator, details);
    }

    private String assembleKey(String separator, Object... details) {
        if (ArrayUtil.isEmpty(details)) {
            return getPrefix();
        }
        int len = details.length;
        StringBuilder builder = new StringBuilder(getPrefix());
        for (int i = 0; i < len; i++) {
            Object detail = details[i];
            if (detail == null) {
                continue;
            }
            if (StrUtil.isBlank(Convert.toStr(detail))) {
                continue;
            }
            builder.append(separator).append(detail);
        }
        return builder.toString();
    }


}
