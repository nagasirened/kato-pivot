package com.kato.pro.constant;

import com.alibaba.fastjson.serializer.JSONSerializable;
import org.apache.curator.x.discovery.details.InstanceSerializer;
import org.apache.curator.x.discovery.details.JsonInstanceSerializer;

/**
 * @ClassName ConstantClass
 * @Author Zeng Guangfu
 * @Description 公共字典
 * @Date 2022/4/1 3:27 下午
 * @Version 1.0
 */
public class ConstantClass {

    /**
     * 魔数
     */
    public static final short MAGIC_NUMBER = 0x01;

    /**
     * 协议版本号
     */
    public static final byte VERSION = 0x1;

    /**
     * 消息头的最低长度
     */
    public static final int LESS_MSG_LENGTH = 29;

    /**
     * curator basePath
     */
    public static final String CURATOR_BASE_PATH = "/kato_discover";

    /**
     * redisson服务注册地址前缀
     */
    public static final String REDISSON_REMOTE_SERVICE_PREFIX = "servicePrefix:";
}
