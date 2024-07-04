package com.kato.pro.common.pool;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.pool2.BaseKeyedPooledObjectFactory;
import org.apache.commons.pool2.KeyedObjectPool;
import org.apache.commons.pool2.PoolUtils;
import org.apache.commons.pool2.impl.GenericKeyedObjectPool;
import org.apache.commons.pool2.impl.GenericKeyedObjectPoolConfig;


@Slf4j
@Data
public class DynamicObjectPool<M extends ClientManager<? extends AutoCloseable>> {

    private final KeyedObjectPool<String, M> clientPool;

    private final String clientKey;

    /**
     * maxTotal          最多连接数
     * minIdle           最小空闲连接数
     * maxIdle           最大空闲连接数
     * maxWaitMillis     最大等待时间
     */
    private Integer maxTotal;
    private Integer minIdle;
    private Integer maxIdle;
    private Integer maxWaitMillis;


    /**
     * @param clientKey         名称
     */
    public DynamicObjectPool(String clientKey, BaseKeyedPooledObjectFactory<String, M> factory) {
        this.clientKey = clientKey;
        GenericKeyedObjectPoolConfig<M> poolConfig = new GenericKeyedObjectPoolConfig<>();
        this.clientPool = PoolUtils.synchronizedPool(new GenericKeyedObjectPool<>(factory, poolConfig));
    }

    /**
     * 借对象
     * @return          ClientManager<T>
     */
    public M borrowClient() {
        try {
            return clientPool.borrowObject(clientKey);
        } catch (Exception e) {
            log.error("borrow client fail, type: {}", clientKey, e);
            return null;
        }
    }

    /**
     * 返还对象，如果开启计数，则达到上限后销毁该对象，下次再需要就自动重建
     * @param m 需要返回的对象
     */
    public void returnClient(M m) {
        try {
            if (m.getLimiter() && m.getCounter().addAndGet(1) > m.getLimitNumber()) {
                clientPool.invalidateObject(clientKey, m);
            } else {
                clientPool.returnObject(clientKey, m);
            }
        } catch ( Exception e ) {
            log.error("return client fail, type: {}", clientKey, e );
        }
    }

}
