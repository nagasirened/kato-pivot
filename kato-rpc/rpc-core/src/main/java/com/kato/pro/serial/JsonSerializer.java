package com.kato.pro.serial;


import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

import java.nio.charset.StandardCharsets;

/**
 * @ClassName JsonSerializer
 * @Author Zeng Guangfu
 * @Description FastJSON
 * @Date 2022/4/1 4:15 下午
 * @Version 1.0
 */
public class JsonSerializer implements Serializer{


    @Override
    public <T> byte[] serialize(T data) {
        return JSON.toJSONString(data).getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public <T> T deserialize(byte[] data, Class<T> clazz) {
        return JSONObject.parseObject(data, clazz);
    }

}
