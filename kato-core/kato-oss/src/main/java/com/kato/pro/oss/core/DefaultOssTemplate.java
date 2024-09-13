package com.kato.pro.oss.core;

import com.aliyun.oss.OSS;
import com.aliyun.oss.model.*;
import com.kato.pro.common.exception.KatoServiceException;
import lombok.extern.slf4j.Slf4j;

import java.io.InputStream;
import java.net.URL;
import java.util.Date;
import java.util.List;

@Slf4j
public class DefaultOssTemplate implements OssTemplate{

    private final OssLinksPoolUtils ossLinksPoolUtils;

    public DefaultOssTemplate(OssLinksPoolUtils ossLinksPoolUtils) {
        this.ossLinksPoolUtils = ossLinksPoolUtils;
    }

    private OSS getClient() {
        OSS ossClient = ossLinksPoolUtils.getOssClient();
        if (ossClient == null) {
            throw new KatoServiceException("get ossClient fail");
        }
        return ossClient;
    }

    private void releaseClient(OSS ossClient) {
        ossLinksPoolUtils.returnOssClient(ossClient);
    }

    public ListObjectsV2Result getObjectsByPrefix(String bucketName, String prefix) {
        OSS client = getClient();
        try {
            return client.listObjectsV2(bucketName, prefix);
        } finally {
            releaseClient(client);
        }
    }

    public URL getObjectUrl(String bucketName, String objectName, Date expires) {
        OSS client = getClient();
        try {
            return client.generatePresignedUrl(bucketName, objectName, expires);
        } finally {
            releaseClient(client);
        }
    }

    public PutObjectResult uploadObject(String bucketName, String objectName, InputStream inputStream) {
        OSS client = getClient();
        try {
            return client.putObject(bucketName, objectName, inputStream);
        } finally {
            releaseClient(client);
        }
    }

    public PutObjectResult uploadObject(String bucketName, String objectName, InputStream inputStream, long size, String contentType) {
        OSS client = getClient();
        try {
            ObjectMetadata objectMetadata = new ObjectMetadata();
            objectMetadata.setContentType(contentType);
            objectMetadata.setContentLength(size);
            return client.putObject(bucketName, objectName, inputStream, objectMetadata);
        } finally {
            releaseClient(client);
        }
    }

    public void removeObject(String bucketName, String objectName) {
        OSS client = getClient();
        try {
            client.deleteObject(bucketName, objectName);
        } finally {
            releaseClient(client);
        }
    }

    public void createBucket(String bucketName) {
        OSS client = getClient();
        try {
            client.createBucket(bucketName);
        } finally {
            releaseClient(client);
        }
    }

    public List<Bucket> getAllBucket() {
        OSS client = getClient();
        try {
            return client.listBuckets();
        } finally {
            releaseClient(client);
        }
    }

    public BucketInfo getBucketByName(String bucketName) {
        OSS client = getClient();
        try {
            return client.getBucketInfo(bucketName);
        } finally {
            releaseClient(client);
        }
    }

    public void removeBucket(String bucketName) {
        OSS client = getClient();
        try {
            client.deleteBucket(bucketName);
        } finally {
            releaseClient(client);
        }
    }

    public OSSObject getObject(String bucketName, String name) {
        OSS client = getClient();
        try {
            return client.getObject(bucketName, name);
        } finally {
            ossLinksPoolUtils.returnOssClient(client);
        }
    }

}
