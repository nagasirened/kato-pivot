package com.kato.pro.rpc;

import cn.hutool.core.convert.Convert;
import com.kato.pro.common.utils.ThrowUtil;
import com.kato.pro.constant.ConstantClass;
import com.kato.pro.constant.RpcRequest;
import com.kato.pro.constant.RpcResponse;
import com.kato.pro.rpc.entity.MessageHeader;
import com.kato.pro.rpc.entity.MessageType;
import com.kato.pro.rpc.entity.RpcProtocol;
import com.kato.pro.serial.Serializer;
import com.kato.pro.serial.SerializerEnum;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;

/**
 * @ClassName DeCodec
 * @Author Zeng Guangfu
 * @Description 消息解析
 * @Date 2022/4/1 4:49 下午
 * @Version 1.0
 */
@Slf4j
public class DeCodec extends ByteToMessageDecoder {

    /**
    +---------------------------------------------------------------+
    | 魔数 2byte | 协议版本号 1byte | 序列化算法 1byte | 报文类型 1byte  |
    +---------------------------------------------------------------+
    | 状态 1byte |        消息 ID 19byte     |      数据长度 4byte     |
    +---------------------------------------------------------------+
    */
    @Override
    protected void decode(ChannelHandlerContext channelHandlerContext, ByteBuf in, List<Object> list) throws Exception {
        if (!in.isReadable() || in.readableBytes() < ConstantClass.LESS_MSG_LENGTH) {
            return;
        }
        // 标记重置位，开启解析
        in.markReaderIndex();
        short magic = in.readShort();
        byte version = in.readByte();
        byte serializerType = in.readByte();
        byte msgType = in.readByte();
        byte status = in.readByte();
        CharSequence reqId = in.readCharSequence(19, StandardCharsets.UTF_8);
        int dataLength = in.readInt();
        // 如果消息长度小于dataLength，代表消息被拆包，等待下一次解析
        if (in.readableBytes() < dataLength) {
            in.resetReaderIndex();
            return;
        }
        // 读取data
        byte[] data = new byte[dataLength];
        in.readBytes(data);
        if (magic != ConstantClass.MAGIC_NUMBER) {
            ThrowUtil.runtimeException("魔数错误");
        }
        // 包装header
        MessageHeader header = MessageHeader.builder()
                .magic(magic).version(version).serialization(serializerType).msgType(msgType)
                .status(status).requestId(Convert.toStr(reqId)).msgLen(dataLength).build();
        // 获取反编译
        Serializer serializer = SerializerEnum.getSerializerByType(serializerType);
        MessageType msgTypeEnum = MessageType.getByType(msgType);
        if (Objects.isNull(msgTypeEnum)) {
            ThrowUtil.runtimeException("不存在的类型");
        }
        switch (msgTypeEnum) {
            case REQUEST:
                RpcProtocol<RpcRequest> reqProtocol = new RpcProtocol<>();
                reqProtocol.setHeader(header);
                RpcRequest rpcRequest = serializer.deserialize(data, RpcRequest.class);
                reqProtocol.setData(rpcRequest);
                list.add(reqProtocol);
                break;
            case RESPONSE:
                RpcProtocol<RpcResponse> respProtocol = new RpcProtocol<>();
                RpcResponse rpcResponse = serializer.deserialize(data, RpcResponse.class);
                respProtocol.setData(rpcResponse);
                respProtocol.setHeader(header);
                list.add(respProtocol);
                break;
            default:
                ThrowUtil.runtimeException("不存在的类型");
        }
    }

}
