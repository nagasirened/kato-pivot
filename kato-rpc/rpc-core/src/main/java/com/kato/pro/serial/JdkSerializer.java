package com.kato.pro.serial;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.charset.StandardCharsets;

/**
 * @ClassName JsonSerializer
 * @Author Zeng Guangfu
 * @Description TODO
 * @Date 2022/4/1 4:07 下午
 * @Version 1.0
 */
@Slf4j
public class JdkSerializer implements Serializer {

    @Override
    public <T> byte[] serialize(T data) {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             ObjectOutputStream oos = new ObjectOutputStream(bos)) {
            oos.writeObject(data);
            return bos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("JDK序列化失败");
        }
    }

    @Override
    public <T> T deserialize(byte[] data, Class<T> clazz) {
        try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(data))){
            return (T) ois.readObject();
        } catch (Exception e) {
            throw new RuntimeException("JDK反序列化失败");
        }
    }
}
