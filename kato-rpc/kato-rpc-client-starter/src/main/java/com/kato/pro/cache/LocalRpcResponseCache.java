package com.kato.pro.cache;

import com.kato.pro.client.transport.RpcFuture;
import com.kato.pro.constant.RpcResponse;
import com.kato.pro.rpc.entity.RpcProtocol;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class LocalRpcResponseCache {

    private static Map<String, RpcFuture<RpcProtocol<RpcResponse>>> requestResponseCache = new ConcurrentHashMap<>();

    /**
     *  添加请求和响应的映射关系
     * @param reqId
     * @param future
     */
    public static void add(String reqId, RpcFuture<RpcProtocol<RpcResponse>> future){
        requestResponseCache.put(reqId, future);
    }

    /**
     *  设置响应数据
     * @param reqId
     * @param messageProtocol
     */
    public static void fillResponse(String reqId, RpcProtocol<RpcResponse> messageProtocol){
        // 获取缓存中的 future
        RpcFuture<RpcProtocol<RpcResponse>> future = requestResponseCache.get(reqId);

        // 设置数据
        future.setResponse(messageProtocol);

        // 移除缓存
        requestResponseCache.remove(reqId);
    }
}