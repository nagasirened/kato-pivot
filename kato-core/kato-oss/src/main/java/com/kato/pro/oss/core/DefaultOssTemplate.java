package com.kato.pro.oss.core;

import com.aliyun.oss.OSS;
import com.aliyun.oss.model.*;
import lombok.extern.slf4j.Slf4j;

import java.io.InputStream;
import java.net.URL;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

@Slf4j
public class DefaultOssTemplate implements OssTemplate{

    private final OSS client;

    public DefaultOssTemplate(OSS ossClient) {
        this.client = ossClient;
    }

    public ListObjectsV2Result getObjectsByPrefix(String bucketName, String prefix) {
        return client.listObjectsV2(bucketName, prefix);
    }

    public URL getObjectUrl(String bucketName, String objectName, Date expires) {
        return client.generatePresignedUrl(bucketName, objectName, expires);
    }

    public PutObjectResult uploadObject(String bucketName, String objectName, InputStream inputStream) {
        return client.putObject(bucketName, objectName, inputStream);
    }

    public PutObjectResult uploadObject(String bucketName, String objectName, InputStream inputStream, long size, String contentType) {
        ObjectMetadata objectMetadata = new ObjectMetadata();
        objectMetadata.setContentType(contentType);
        objectMetadata.setContentLength(size);
        return client.putObject(bucketName, objectName, inputStream, objectMetadata);
    }

    public void removeObject(String bucketName, String objectName) {
        client.deleteObject(bucketName, objectName);
    }

    public void createBucket(String bucketName) {
        client.createBucket(bucketName);
    }

    public List<Bucket> getAllBucket() {
        return client.listBuckets();
    }

    public BucketInfo getBucketByName(String bucketName) {
        return client.getBucketInfo(bucketName);
    }

    public void removeBucket(String bucketName) {
        client.deleteBucket(bucketName);
    }

    public OSSObject getObject(String bucketName, String name) {
        return client.getObject(bucketName, name);
    }

}
