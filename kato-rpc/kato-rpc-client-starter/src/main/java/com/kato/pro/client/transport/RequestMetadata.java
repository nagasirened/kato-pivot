package com.kato.pro.client.transport;

import com.kato.pro.constant.RpcRequest;
import com.kato.pro.rpc.entity.RpcProtocol;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

@Data
@Accessors(chain = true)
public class RequestMetadata implements Serializable {

    /**
     * 请求
     */
    private RpcProtocol<RpcRequest> protocol;

    /**
     * 请求地址
     */
    private String address;

    /**
     * 连接端口
     */
    private Integer port;

    /**
     * 服务调用超时时间
     */
    private Integer timeout;

}
