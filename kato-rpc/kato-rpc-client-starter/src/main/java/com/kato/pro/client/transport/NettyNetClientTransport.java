package com.kato.pro.client.transport;

import com.kato.pro.cache.LocalRpcResponseCache;
import com.kato.pro.constant.RpcRequest;
import com.kato.pro.constant.RpcResponse;
import com.kato.pro.handler.RpcResponseHandler;
import com.kato.pro.rpc.DeCodec;
import com.kato.pro.rpc.EnCodec;
import com.kato.pro.rpc.entity.RpcProtocol;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;

@Slf4j
public class NettyNetClientTransport implements NetClientTransport {

    private final Bootstrap bootstrap;
    private final EventLoopGroup eventLoopGroup;
    private final RpcResponseHandler responseHandler;

    public NettyNetClientTransport() {
        bootstrap = new Bootstrap();
        eventLoopGroup = new NioEventLoopGroup(4);
        responseHandler = new RpcResponseHandler();
        bootstrap.group(eventLoopGroup)
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel sc) throws Exception {
                        ChannelPipeline pipeline = sc.pipeline();
                        pipeline.addLast(new EnCodec<>())
                                .addLast(new DeCodec())
                                .addLast(responseHandler);
                    }
                });
    }

    @Override
    public RpcProtocol<RpcResponse> sendRequest(RequestMetadata metadata) throws Exception {
        RpcProtocol<RpcRequest> protocol = metadata.getProtocol();

        RpcFuture<RpcProtocol<RpcResponse>> future = new RpcFuture<>();
        LocalRpcResponseCache.add(protocol.getHeader().getRequestId(), future);

        ChannelFuture channelFuture = bootstrap.connect(metadata.getAddress(), metadata.getPort()).sync();
        channelFuture.addListener((arg) -> {
            if (channelFuture.isSuccess()) {
                log.info("connect rpc server {} on port {} success.", metadata.getAddress(), metadata.getPort());
            } else {
                log.error("connect rpc server {} on port {} failed.", metadata.getAddress(), metadata.getPort());
                channelFuture.cause().printStackTrace();
                eventLoopGroup.shutdownGracefully();
            }
        });

        channelFuture.channel().writeAndFlush(protocol);
        Integer timeout = metadata.getTimeout();
        return timeout != null && timeout > 0 ? future.get(metadata.getTimeout(), TimeUnit.MILLISECONDS) : future.get();
    }
}
