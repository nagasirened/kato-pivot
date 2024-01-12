package com.kato.pro.client.transport;

import com.kato.pro.constant.RpcResponse;
import com.kato.pro.rpc.entity.RpcProtocol;

public interface NetClientTransport {

    /**
     * 发送数据
     * @param metadata
     * @return
     * @throws Exception
     */
    RpcProtocol<RpcResponse> sendRequest(RequestMetadata metadata) throws Exception;

}
