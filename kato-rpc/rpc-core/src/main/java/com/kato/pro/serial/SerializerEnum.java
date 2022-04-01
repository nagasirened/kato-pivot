package com.kato.pro.serial;

import cn.hutool.core.util.StrUtil;
import lombok.Getter;
import org.springframework.util.StringUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * @ClassName SerializerEnum
 * @Author Zeng Guangfu
 * @Description 序列化器枚举
 * @Date 2022/4/1 4:27 下午
 * @Version 1.0
 */
public enum SerializerEnum {

    JDK((byte)1, new JdkSerializer()),
    JSON((byte)2, new JsonSerializer()),
    HESSIAN((byte)3, new HessianSerializer()),
    ;

    @Getter
    private final byte serializerType;
    @Getter
    private final Serializer serializer;

    SerializerEnum(byte serializerType, Serializer serializer) {
        this.serializerType = serializerType;
        this.serializer = serializer;
    }

    public static Serializer getSerializerByName(String name) {
        for (SerializerEnum item : SerializerEnum.values()) {
            if (StrUtil.equalsIgnoreCase(item.name(), name)) {
                return item.getSerializer();
            }
        }
        return HESSIAN.serializer;
    }

    public static Serializer getSerializerByType(byte type) {
        for (SerializerEnum item : SerializerEnum.values()) {
            if (item.getSerializerType() == type) {
                return item.getSerializer();
            }
        }
        return HESSIAN.serializer;
    }
}
