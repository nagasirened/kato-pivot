package com.kato.pro.constant;

import lombok.Data;

import java.io.Serializable;

/**
 * @ClassName RpcResponse
 * @Author Zeng Guangfu
 * @Description RPC结果
 * @Date 2022/4/1 9:55 上午
 * @Version 1.0
 */
@Data
public class RpcResponse implements Serializable {

    /**
     * code
     */
    private Integer code;

    /**
     * 信息
     */
    private String message;

    /**
     * 结果
     */
    private Object data;
}
