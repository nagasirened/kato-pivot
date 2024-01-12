package com.kato.pro.rpc;

import io.netty.handler.codec.LengthFieldBasedFrameDecoder;

public class KatoLengthFiledHandler extends LengthFieldBasedFrameDecoder {


    /**
     +---------------------------------------------------------------+
     | 魔数 2byte | 协议版本号 1byte | 序列化算法 1byte | 报文类型 1byte  |
     +---------------------------------------------------------------+
     | 状态 1byte |        消息 ID 19byte     |      数据长度 4byte     |
     +---------------------------------------------------------------+
     */

    public KatoLengthFiledHandler() {
        this(1024 * 1024, 25,4,0,0);
    }


    public KatoLengthFiledHandler(int maxFrameLength, int lengthFieldOffset, int lengthFieldLength, int lengthAdjustment, int initialBytesToStrip) {
        super(maxFrameLength, lengthFieldOffset, lengthFieldLength, lengthAdjustment, initialBytesToStrip);
    }
}
