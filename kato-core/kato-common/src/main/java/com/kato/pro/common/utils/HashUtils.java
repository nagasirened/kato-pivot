package com.kato.pro.common.utils;

import cn.hutool.core.lang.hash.MurmurHash;

public class HashUtils {

    private int murmur32Hash(String item) {
        return Math.abs(MurmurHash.hash32(item.getBytes()));
    }

}
