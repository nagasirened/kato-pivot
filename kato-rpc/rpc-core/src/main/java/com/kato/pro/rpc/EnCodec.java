package com.kato.pro.rpc;

import com.kato.pro.rpc.entity.MessageHeader;
import com.kato.pro.rpc.entity.RpcProtocol;
import com.kato.pro.serial.SerializerEnum;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;

/**
 * @ClassName EnCodec
 * @Author Zeng Guangfu
 * @Description Msg -> ByteBuf
 * @Date 2022/4/1 3:23 下午
 * @Version 1.0
 */
@Slf4j
public class EnCodec<T> extends MessageToByteEncoder<RpcProtocol<T>> {

    /**
    +---------------------------------------------------------------+
    | 魔数 2byte | 协议版本号 1byte | 序列化算法 1byte | 报文类型 1byte  |
    +---------------------------------------------------------------+
    | 状态 1byte |        消息 ID 19byte     |      数据长度 4byte     |
    +---------------------------------------------------------------+
    */
    @Override
    protected void encode(ChannelHandlerContext channelHandlerContext, RpcProtocol<T> rpcRequestRpcProtocol, ByteBuf byteBuf) throws Exception {
        MessageHeader header = rpcRequestRpcProtocol.getHeader();
        // 魔数
        byteBuf.writeShort(header.getMagic());
        // 版本号
        byteBuf.writeByte(header.getVersion());
        // 序列化版本
        byteBuf.writeByte(header.getSerialization());
        // 数据类型
        byteBuf.writeByte(header.getMsgType());
        // 状态
        byteBuf.writeByte(header.getStatus());
        // 唯一请求号
        byteBuf.writeCharSequence(header.getRequestId(), StandardCharsets.UTF_8);
        // 转换消息，消息长度和消息
        T data = rpcRequestRpcProtocol.getData();
        byte[] bytes = SerializerEnum.getSerializerByType(header.getSerialization()).serialize(data);
        byteBuf.writeInt(bytes.length);
        byteBuf.writeBytes(bytes);
    }
}
