package com.kato.pro.s3.aws.impl;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.*;
import com.amazonaws.util.IOUtils;
import com.kato.pro.s3.config.S3Template;
import lombok.SneakyThrows;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.*;

public class AwsS3Template implements S3Template {

    private final AmazonS3 amazonS3;

    public AwsS3Template(AmazonS3 amazonS3) {
        this.amazonS3 = amazonS3;
    }

    /**
     * 根据前缀查询文件
     *
     * @param bucketName      桶
     * @param prefix          文件前缀
     * @return
     */
    @SneakyThrows
    public List<S3ObjectSummary> getObjectsByPrefix(String bucketName, String prefix) {
        ObjectListing objectListing = amazonS3.listObjects(bucketName, prefix);
        return objectListing.getObjectSummaries();
    }

    /**
     * 获取文件外链
     *
     * @param bucketName        桶
     * @param objectName        文件名
     * @param expires           过期时间,默认7天
     */
    @SneakyThrows
    public String getObjectUrl(String bucketName, String objectName, Integer expires) {
        DateTime dateTime = DateUtil.offsetDay(new Date(), 7);
        URL url = amazonS3.generatePresignedUrl(bucketName, objectName, dateTime.toJdkDate());
        return Objects.nonNull(url) ? url.toString() : "";
    }

    /**
     * 获取文件
     *
     * @param bucketName        桶
     * @param objectName        文件名
     */
    @SneakyThrows
    public S3Object getObject(String bucketName, String objectName) {
        return amazonS3.getObject(bucketName, objectName);
    }

    /**
     * 获取文件流
     *
     * @param bucketName        桶
     * @param objectName        文件名
     */
    @SneakyThrows
    public InputStream getObjectInputStream(String bucketName, String objectName) {
        S3Object s3Object = amazonS3.getObject(bucketName, objectName);
        return Objects.nonNull(s3Object) ? s3Object.getObjectContent() : null;
    }

    /**
     * 上传文件
     *
     * @param bucketName        桶
     * @param objectName        文件名
     * @param inputStream       文件流
     */
    @SneakyThrows
    public void uploadObject(String bucketName, String objectName, InputStream inputStream) {
        uploadObject(bucketName, objectName, inputStream, inputStream.available(), "application/octet-stream");
    }

    @SneakyThrows
    public PutObjectResult uploadObject(String bucketName, String objectName, InputStream inputStream, long size, String contentType) {
        byte[] bytes = IOUtils.toByteArray(inputStream);
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(size);
        metadata.setContentType(contentType);
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(bytes);
        return amazonS3.putObject(bucketName, objectName, byteArrayInputStream, metadata);
    }

    /**
     * 删除文件
     *
     * @param bucketName    桶
     * @param objectName    文件名称
     */
    @SneakyThrows
    public void removeObject(String bucketName, String objectName) {
        amazonS3.deleteObject(bucketName, objectName);
    }

    /**
     * 创建Bucket
     *
     * @param bucketName    桶名称
     */
    @SneakyThrows
    public void createBucket(String bucketName) {
        if (!amazonS3.doesBucketExistV2(bucketName)) {
            amazonS3.createBucket(bucketName);
        }
    }

    /**
     * 获取全部Bucket信息
     */
    @SneakyThrows
    public List<Bucket> getAllBucket() {
        return amazonS3.listBuckets();
    }

    /**
     * 根据bucketName获取对应的Optional<Bucket>
     *
     * @param bucketName        桶名称
     */
    @SneakyThrows
    public Optional<Bucket> getBucketByName(String bucketName) {
        return amazonS3.listBuckets().stream()
                .filter(bucket -> StrUtil.equals(bucket.getName(), bucketName))
                .findFirst();
    }

    /**
     *  删除bucket
     *
     * @param bucketName        桶名称
     */
    @SneakyThrows
    public void removeBucket(String bucketName) {
        amazonS3.deleteBucket(bucketName);
    }

}
