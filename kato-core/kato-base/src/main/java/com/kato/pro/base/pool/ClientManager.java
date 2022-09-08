package com.kato.pro.base.pool;

import lombok.Data;

import java.io.Closeable;
import java.util.concurrent.atomic.AtomicInteger;

@Data
public class ClientManager<T extends AutoCloseable> {

    private final T client;

    private final AtomicInteger counter;

    private final Boolean limiter;

    public final Integer limitNumber;

    public ClientManager(T client) {
        this(client, true);
    }

    public ClientManager(T client, Boolean isLimit) {
        this(client, isLimit, 10000);
    }

    public ClientManager(T client, Boolean isLimit, Integer limitNumber) {
        this.client = client;
        this.limiter = isLimit;
        this.counter = new AtomicInteger(0);
        this.limitNumber = limitNumber < 10 ? 10 : limitNumber;
    }

}
