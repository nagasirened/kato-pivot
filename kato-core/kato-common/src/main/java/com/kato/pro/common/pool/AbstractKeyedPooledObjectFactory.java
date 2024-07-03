package com.kato.pro.common.pool;

import org.apache.commons.pool2.BaseKeyedPooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;

import java.util.Objects;

public abstract class AbstractKeyedPooledObjectFactory<T extends AutoCloseable> extends BaseKeyedPooledObjectFactory<String, ClientManager<T>> {

    @Override
    public ClientManager<T> create(String type) throws Exception {
        return new ClientManager<>(generalClient(type), true);
    }

    @Override
    public PooledObject<ClientManager<T>> wrap(ClientManager<T> clientManager) {
        return new DefaultPooledObject<>(clientManager);
    }

    @Override
    public void destroyObject(String key, PooledObject<ClientManager<T>> p) throws Exception {
        T client = p.getObject().getClient();
        if (Objects.nonNull(client)) {
            client.close();
        }
        super.destroyObject(key, p);
    }

    public abstract T generalClient(String type);

}
