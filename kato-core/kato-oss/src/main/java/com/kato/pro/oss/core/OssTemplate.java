package com.kato.pro.oss.core;

import com.aliyun.oss.model.*;

import java.io.InputStream;
import java.net.URL;
import java.util.Date;
import java.util.List;

public interface OssTemplate {

    ListObjectsV2Result getObjectsByPrefix(String bucketName, String prefix);

    URL getObjectUrl(String bucketName, String objectName, Date expires);

    PutObjectResult uploadObject(String bucketName, String objectName, InputStream inputStream);

    PutObjectResult uploadObject(String bucketName, String objectName, InputStream inputStream, long size, String contentType);

    void removeObject(String bucketName, String objectName);

    void createBucket(String bucketName);

    List<Bucket> getAllBucket();

    BucketInfo getBucketByName(String bucketName);

    void removeBucket(String bucketName);

    OSSObject getObject(String bucketName, String name);

}
