package com.kato.pro.common.utils;

import cn.hutool.core.lang.hash.MurmurHash;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class HashUtils {

    public static int murmur32Hash(String item) {
        return Math.abs(MurmurHash.hash32(item.getBytes()));
    }

}
