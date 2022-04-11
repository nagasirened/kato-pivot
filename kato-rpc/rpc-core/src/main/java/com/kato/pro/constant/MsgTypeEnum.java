package com.kato.pro.constant;

/**
 * author: ZGF
 */

public enum  MsgTypeEnum {

    REQUEST((byte)1),
    RESPONSE((byte)2),
    ;

    private byte type;

    MsgTypeEnum(byte type) {
        this.type = type;
    }

    public static MsgTypeEnum getByType(byte type) {
        for (MsgTypeEnum msgTypeEnum : MsgTypeEnum.values()) {
            if (msgTypeEnum.type == type) {
                return msgTypeEnum;
            }
        }
        return null;
    }

}
