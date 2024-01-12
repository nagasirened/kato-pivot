package com.kato.pro.oss.core;

import com.aliyun.oss.OSS;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.pool2.KeyedObjectPool;
import org.apache.commons.pool2.PoolUtils;
import org.apache.commons.pool2.impl.GenericKeyedObjectPool;
import org.apache.commons.pool2.impl.GenericKeyedObjectPoolConfig;

import javax.annotation.PostConstruct;

@Slf4j
public class OssLinksPoolUtils {

    public static final String OSS_KEY = "kato_oss";

    private KeyedObjectPool<String, OSS> ossPool = null;

    private final OssClientFactory ossClientFactory;

    public OssLinksPoolUtils(OssClientFactory ossClientFactory) {
        this.ossClientFactory = ossClientFactory;
    }

    @PostConstruct
    public void init() {
        GenericKeyedObjectPoolConfig<OSS> poolConfig = new GenericKeyedObjectPoolConfig<>();
        poolConfig.setMaxTotalPerKey(10);   // 单个模型最多连接数
        poolConfig.setMinIdlePerKey(0);     // 单个模型最小空闲连接数
        poolConfig.setMaxIdlePerKey(5);     // 单个模型最多空闲连接数
        poolConfig.setMaxWaitMillis(500);   // 最大等待时间
        poolConfig.setLifo(true);           // 连接池存放池化对象，true放在空闲队列前面，false放在空闲队列后面
        ossPool = PoolUtils.synchronizedPool(new GenericKeyedObjectPool<>(ossClientFactory, poolConfig));
    }

    public OSS getOssClient() {
        try {
            return ossPool.borrowObject(OSS_KEY);
        } catch (Exception e) {
            log.error("getOssClient error", e);
            return null;
        }
    }

    public void returnOssClient(OSS ossClient) {
        try {
            ossPool.returnObject(OSS_KEY, ossClient);
        } catch (Exception e) {
            log.error("returnOssClient error", e);
        }
    }


}
