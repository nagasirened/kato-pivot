package com.kato.pro.serial;

import cn.hutool.core.util.StrUtil;
import lombok.Getter;

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

    public static SerializerEnum getSerializerByName(String name) {
        for (SerializerEnum item : SerializerEnum.values()) {
            if (StrUtil.equalsIgnoreCase(item.name(), name)) {
                return item;
            }
        }
        return HESSIAN;
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
