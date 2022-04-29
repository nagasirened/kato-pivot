package com.kato.pro.aws;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.amazonaws.services.s3.model.*;
import com.amazonaws.util.IOUtils;
import lombok.Cleanup;
import lombok.SneakyThrows;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public interface AwsTemplate {

    public List<S3ObjectSummary> getObjectsByPrefix(String bucketName, String prefix);

    public String getObjectUrl(String bucketName, String objectName, Integer expires);

    public S3Object getObject(String bucketName, String objectName);

    public InputStream getObjectInputStream(String bucketName, String objectName);

    public void uploadObject(String bucketName, String objectName, InputStream inputStream);

    public PutObjectResult uploadObject(String bucketName, String objectName, InputStream inputStream, long size, String contentType);

    public void removeObject(String bucketName, String objectName);

    public void createBucket(String bucketName);

    public List<Bucket> getAllBucket();

    public Optional<Bucket> getBucketByName(String bucketName);

    public void removeBucket(String bucketName);
}
