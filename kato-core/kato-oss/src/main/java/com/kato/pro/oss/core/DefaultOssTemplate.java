package com.kato.pro.oss.core;

import com.aliyun.oss.OSS;
import com.aliyun.oss.model.*;
import lombok.extern.slf4j.Slf4j;

import java.io.InputStream;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

@Slf4j
public abstract class DefaultOssTemplate implements OssTemplate{

    private final OssLinksPoolUtils ossLinksPoolUtils;

    public DefaultOssTemplate(OssLinksPoolUtils ossLinksPoolUtils) {
        this.ossLinksPoolUtils = ossLinksPoolUtils;
    }

    private OSS getClient() {
        OSS ossClient = ossLinksPoolUtils.getOssClient();
        if (ossClient == null) {
            log.error("get ossClient fail");
            throw new RuntimeException("system error");
        }
        return ossClient;
    }

    abstract ListObjectsV2Result getObjectsByPrefix(String bucketName, String prefix);

    abstract String getObjectUrl(String bucketName, String objectName, Integer expires);

    abstract InputStream getObjectInputStream(String bucketName, String objectName);

    abstract void uploadObject(String bucketName, String objectName, InputStream inputStream);

    abstract PutObjectResult uploadObject(String bucketName, String objectName, InputStream inputStream, long size, String contentType);

    abstract void removeObject(String bucketName, String objectName);

    abstract void createBucket(String bucketName);

    abstract List<Bucket> getAllBucket();

    abstract Optional<Bucket> getBucketByName(String bucketName);

    abstract void removeBucket(String bucketName);

    OSSObject getObject(String bucketName, String name) {
        OSS client = getClient();
        try {
            return client.getObject(bucketName, name);
        } finally {
            ossLinksPoolUtils.returnOssClient(client);
        }
    }


}
