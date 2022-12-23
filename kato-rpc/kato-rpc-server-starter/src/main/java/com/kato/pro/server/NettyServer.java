package com.kato.pro.server;

import com.kato.pro.rpc.DeCodec;
import com.kato.pro.rpc.EnCodec;
import com.kato.pro.rpc.KatoLengthFiledHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import lombok.extern.slf4j.Slf4j;

import java.net.InetAddress;

@Slf4j
public class NettyServer implements RpcServer {

    /**
     * 启动服务端
     * @param port  netty 端口
     */
    @Override
    public void init(Integer port) {
        NioEventLoopGroup boss = new NioEventLoopGroup(1);
        NioEventLoopGroup worker = new NioEventLoopGroup(3);

        NioEventLoopGroup reqEventLoopGroup = new NioEventLoopGroup(Math.max(Runtime.getRuntime().availableProcessors(), 10));
        try {
            ServerBootstrap serverBootstrap = new ServerBootstrap()
                    .group(boss, worker)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<NioSocketChannel>() {
                        @Override
                        protected void initChannel(NioSocketChannel ch) throws Exception {
                            ChannelPipeline pipeline = ch.pipeline();
                            pipeline.addLast(new KatoLengthFiledHandler())
                                    .addLast("outboundEeCodec", new EnCodec<>())
                                    .addLast("inboundDeCodec", new DeCodec())
                                    .addLast(reqEventLoopGroup, "reqEventLoopGroup", new KatoRequestHandler());
                        }
                    })
                    .childOption(ChannelOption.SO_KEEPALIVE, true);
            String hostAddress = InetAddress.getLocalHost().getHostAddress();
            ChannelFuture channelFuture = serverBootstrap.bind(hostAddress, port).sync();
            log.info("server addr {} started on port {}", hostAddress, port);
            channelFuture.channel().closeFuture().sync();
        } catch (Exception e) {
            log.error("netty server init error", e);
        } finally {
            boss.shutdownGracefully();
            worker.shutdownGracefully();
        }
    }

}
