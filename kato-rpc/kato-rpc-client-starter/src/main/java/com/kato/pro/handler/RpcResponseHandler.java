package com.kato.pro.handler;

import com.kato.pro.cache.LocalRpcResponseCache;
import com.kato.pro.constant.RpcResponse;
import com.kato.pro.rpc.entity.RpcProtocol;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

public class RpcResponseHandler extends SimpleChannelInboundHandler<RpcProtocol<RpcResponse>> {
    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, RpcProtocol<RpcResponse> rpcResponseRpcProtocol) throws Exception {
        String reqId = rpcResponseRpcProtocol.getHeader().getRequestId();
        LocalRpcResponseCache.fillResponse(reqId, rpcResponseRpcProtocol);
    }
}
