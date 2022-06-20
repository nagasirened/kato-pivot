package com.kato.pro.oss;

import com.aliyun.oss.OSS;
import com.aliyun.oss.model.OSSObject;
import com.kato.pro.res.ErrorCode;
import com.kato.pro.res.KatoServiceException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DefaultOssTemplate implements OssTemplate{

    private final OssLinksPoolUtils ossLinksPoolUtils;

    public DefaultOssTemplate(OssLinksPoolUtils ossLinksPoolUtils) {
        this.ossLinksPoolUtils = ossLinksPoolUtils;
    }

    private OSS getClient() {
        OSS ossClient = ossLinksPoolUtils.getOssClient();
        if (ossClient == null) {
            log.error("get ossClient fail");
            throw new KatoServiceException(ErrorCode.SYSTEM_ERROR);
        }
        return ossClient;
    }




    /*List<S3ObjectSummary> getObjectsByPrefix(String bucketName, String prefix);

    String getObjectUrl(String bucketName, String objectName, Integer expires);

    S3Object getObject(String bucketName, String objectName);

    InputStream getObjectInputStream(String bucketName, String objectName);

    void uploadObject(String bucketName, String objectName, InputStream inputStream);

    PutObjectResult uploadObject(String bucketName, String objectName, InputStream inputStream, long size, String contentType);

    void removeObject(String bucketName, String objectName);

    void createBucket(String bucketName);

    List<Bucket> getAllBucket();

    Optional<Bucket> getBucketByName(String bucketName);

    void removeBucket(String bucketName);*/

    OSSObject getObject(String bucketName, String name) {
        OSS client = getClient();
        try {
            return client.getObject(bucketName, name);
        } finally {
            ossLinksPoolUtils.returnOssClient(client);
        }
    }


}
