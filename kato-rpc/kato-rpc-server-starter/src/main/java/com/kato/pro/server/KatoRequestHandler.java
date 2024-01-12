package com.kato.pro.server;

import com.kato.pro.constant.RpcRequest;
import com.kato.pro.constant.RpcResponse;
import com.kato.pro.rpc.entity.MessageHeader;
import com.kato.pro.rpc.entity.MessageType;
import com.kato.pro.rpc.entity.ResultStatus;
import com.kato.pro.rpc.entity.RpcProtocol;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Objects;

/**
 * SimpleChannelInboundHandler 代表该入参处理器，仅处理泛型类型的内容
 */
@Slf4j
public class KatoRequestHandler extends SimpleChannelInboundHandler<RpcProtocol<RpcRequest>> {

    @Override
    protected void channelRead0(ChannelHandlerContext context, RpcProtocol<RpcRequest> rpcRequestRpcProtocol) throws Exception {
        MessageHeader header = rpcRequestRpcProtocol.getHeader();
        RpcRequest rpcRequest = rpcRequestRpcProtocol.getData();

        // 返回的结果
        RpcProtocol<RpcResponse> rpcProtocol = new RpcProtocol<>();
        RpcResponse rpcResponse = new RpcResponse();
        try {
            header.setStatus(ResultStatus.SUCCESS.getCode());
            rpcResponse.setData(handle(rpcRequest));
        } catch (Exception e) {
            header.setStatus(ResultStatus.FAIL.getCode());
            rpcResponse.setMessage(e.getMessage());
            log.error("request handler exception happened", e);
        }
        header.setMsgType(MessageType.RESPONSE.getType());
        rpcProtocol.setHeader(header);
        rpcProtocol.setData(rpcResponse);
        context.channel().writeAndFlush(rpcProtocol);
    }

    /**
     * 处理请求
     * 1. 根据serviceVersion(服务名称 + version)找到服务的接口
     * 2. 根据接口和method和参数集合找到唯一的方法
     * 3. invoke方法
     */
    private Object handle(RpcRequest rpcRequest) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Object bean = LocalServiceCache.get(rpcRequest.getServiceVersion());
        if (Objects.isNull(bean)) {
            return null;
        }
        Method method = bean.getClass().getMethod(rpcRequest.getMethod(), rpcRequest.getParameterTypes());
        return method.invoke(bean, rpcRequest.getParameters());
    }
}
