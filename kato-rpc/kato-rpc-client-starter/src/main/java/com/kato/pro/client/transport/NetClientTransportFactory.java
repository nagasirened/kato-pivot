package com.kato.pro.client.transport;

public class NetClientTransportFactory {

    public static NetClientTransport getNetClientTransport() {
        return new NettyNetClientTransport();
    }

}
