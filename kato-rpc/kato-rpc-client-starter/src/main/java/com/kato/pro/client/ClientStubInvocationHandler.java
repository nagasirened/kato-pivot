package com.kato.pro.client;

import com.kato.pro.client.transport.NetClientTransportFactory;
import com.kato.pro.client.transport.RequestMetadata;
import com.kato.pro.config.KatoClientProperties;
import com.kato.pro.constant.RpcRequest;
import com.kato.pro.constant.RpcResponse;
import com.kato.pro.constant.ServiceInfo;
import com.kato.pro.discovery.DiscoveryService;
import com.kato.pro.rpc.entity.MessageHeader;
import com.kato.pro.rpc.entity.RpcProtocol;
import lombok.extern.slf4j.Slf4j;
import sun.plugin2.message.transport.TransportFactory;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Objects;

@Slf4j
class ClientStubInvocationHandler implements InvocationHandler {

    private DiscoveryService discoveryService;
    private Class<?> clazz;
    private String version;
    private KatoClientProperties clientProperties;

    public ClientStubInvocationHandler(DiscoveryService discoveryService, Class<?> clazz, String version, KatoClientProperties clientProperties) {
        this.discoveryService = discoveryService;
        this.clazz = clazz;
        this.version = version;
        this.clientProperties =clientProperties;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        // 首先发现服务
        String serviceVersion = String.join("_", clazz.getName(), version);
        ServiceInfo serviceInfo = discoveryService.discover(serviceVersion);
        if (Objects.isNull(serviceInfo)) {
            log.error("invoke discover serviceVersion not exists, sv: {}", serviceVersion);
        }

        // 封装请求
        RpcProtocol<RpcRequest> requestRpcProtocol = new RpcProtocol<>();
        // - header
        MessageHeader messageHeader = MessageHeader.getInstance(clientProperties.getSerialize());
        requestRpcProtocol.setHeader(messageHeader);
        // - body
        RpcRequest rpcRequest = new RpcRequest();
        rpcRequest.setMethod(method.getName());
        rpcRequest.setParameters(args);
        rpcRequest.setParameterTypes(method.getParameterTypes());
        rpcRequest.setServiceVersion(serviceVersion);
        requestRpcProtocol.setData(rpcRequest);

        // 由NettyClient开启调用，地址在serviceInfo中
        RequestMetadata requestMetadata = new RequestMetadata().setProtocol(requestRpcProtocol)
                .setAddress(serviceInfo.getAddress())
                .setPort(serviceInfo.getPort())
                .setTimeout(clientProperties.getTimeout());
        RpcProtocol<RpcResponse> responseRpcProtocol = NetClientTransportFactory.getNetClientTransport().sendRequest(requestMetadata);
        if (responseRpcProtocol == null) {
            log.error("请求超时");
            throw new RuntimeException("rpc调用结果失败， 请求超时 timeout:" + clientProperties.getTimeout());
        }

        if (responseRpcProtocol.getHeader().getStatus() != 0) {
            log.error("rpc调用结果失败， message：{}", responseRpcProtocol.getData().getMessage());
            throw new RuntimeException(responseRpcProtocol.getData().getMessage());
        }

        return responseRpcProtocol.getData().getData();
    }
}
