package com.kato.pro.s3.config;

import com.amazonaws.services.s3.model.*;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;

public interface S3Template {

     List<S3ObjectSummary> getObjectsByPrefix(String bucketName, String prefix);

     String getObjectUrl(String bucketName, String objectName, Integer expires);

     S3Object getObject(String bucketName, String objectName);

     InputStream getObjectInputStream(String bucketName, String objectName);

     void uploadObject(String bucketName, String objectName, InputStream inputStream);

     PutObjectResult uploadObject(String bucketName, String objectName, InputStream inputStream, long size, String contentType);

     void removeObject(String bucketName, String objectName);

     void createBucket(String bucketName);

     List<Bucket> getAllBucket();

     Optional<Bucket> getBucketByName(String bucketName);

     void removeBucket(String bucketName);
}
