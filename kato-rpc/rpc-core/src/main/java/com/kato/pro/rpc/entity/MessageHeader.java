package com.kato.pro.rpc.entity;

import lombok.Builder;
import lombok.Data;

/**
 * @ClassName MessageHeader
 * @Author Zeng Guangfu
 * @Description 消息头
 * @Date 2022/4/1 3:25 下午
 * @Version 1.0
 */

@Data
@Builder
public class MessageHeader {

    /*
    +---------------------------------------------------------------+
    | 魔数 2byte | 协议版本号 1byte | 序列化算法 1byte | 报文类型 1byte  |
    +---------------------------------------------------------------+
    | 状态 1byte |        消息 ID 19byte     |      数据长度 4byte     |
    +---------------------------------------------------------------+
    */
    /**
     *  魔数
     */
    private short magic;

    /**
     *  协议版本号
     */
    private byte version;

    /**
     *  序列化算法
     */
    private byte serialization;

    /**
     *  报文类型 req/resp
     */
    private byte msgType;

    /**
     *  状态
     */
    private byte status;

    /**
     *  消息 ID
     */
    private String requestId;

    /**
     *  数据长度
     */
    private int msgLen;
}
