package com.kato.pro.rpc.entity;

import lombok.Data;

import java.io.Serializable;

/**
 * @ClassName RpcProtocol
 * @Author Zeng Guangfu
 * @Description 消息包装类
 * @Date 2022/4/1 3:24 下午
 * @Version 1.0
 */

@Data
public class RpcProtocol<T> implements Serializable {

    private MessageHeader header;

    private T data;

}
