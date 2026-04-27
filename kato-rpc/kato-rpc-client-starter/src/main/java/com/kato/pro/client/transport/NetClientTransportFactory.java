package com.kato.pro.client.transport;

public class NetClientTransportFactory {

    private static final NetClientTransport NET_CLIENT_TRANSPORT = new NettyNetClientTransport();

    public static NetClientTransport getNetClientTransport() {
        return NET_CLIENT_TRANSPORT;
    }

}
