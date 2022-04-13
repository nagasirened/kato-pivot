package com.kato.pro.rpc.entity;

import lombok.Getter;

public enum MessageType {

    REQUEST((byte) 1),
    RESPONSE((byte) 2),
    ;

    @Getter
    private final byte type;

    MessageType(byte type) {
        this.type = type;
    }

    public static MessageType getByType(byte msgType) {
        for (MessageType messageType : MessageType.values()) {
            if (messageType.getType() == msgType) {
                return messageType;
            }
        }
        return null;
    }

}
