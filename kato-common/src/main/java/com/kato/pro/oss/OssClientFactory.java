package com.kato.pro.oss;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import org.apache.commons.pool2.BaseKeyedPooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;

import java.util.Objects;

public class OssClientFactory extends BaseKeyedPooledObjectFactory<String, OSS> {

    private OssProperties ossProperties;

    public OssClientFactory(OssProperties ossProperties) {
        this.ossProperties = ossProperties;
    }

    /**
     * 创建OSS对象
     */
    @Override
    public OSS create(String s) throws Exception {
        return new OSSClientBuilder().build(ossProperties.getEndpoint(), ossProperties.getAccessKeyId(), ossProperties.getAccessKeySecret());
    }

    /**
     * 封装资源池
     */
    @Override
    public PooledObject<OSS> wrap(OSS oss) {
        return new DefaultPooledObject<>(oss);
    }

    /**
     * 关闭
     */
    @Override
    public void destroyObject(String key, PooledObject<OSS> p) throws Exception {
        if (Objects.nonNull(p.getObject())) {
            p.getObject().shutdown();
        }
        super.destroyObject(key, p);
    }
}
