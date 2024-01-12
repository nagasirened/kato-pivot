package com.kato.pro.serial;

/**
 * @ClassName serialize
 * @Author Zeng Guangfu
 * @Description 序列化接口
 * @Date 2022/4/1 4:02 下午
 * @Version 1.0
 */
public interface Serializer {

    <T> byte[] serialize(T data);

    <T> T deserialize(byte[] data, Class<T> clazz);

}
