package com.kato.pro.oss.repository;

import cn.hutool.core.util.StrUtil;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.OSSObject;
import com.kato.pro.oss.core.OssProperties;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
public class OssRepository {

    private final String endpoint;
    private final String accessKeyId;
    private final String accessKeySecret;
    private final String bucket;
    public OssRepository(OssProperties ossProperties) {
        this.endpoint = ossProperties.getEndpoint();
        this.accessKeyId = ossProperties.getAccessKeyId();
        this.accessKeySecret = ossProperties.getAccessKeySecret();
        this.bucket = ossProperties.getBucketName();
    }


    public OSSObject loadFile(String fileName) {
        return loadFile(fileName, null);
    }

    public OSSObject loadFile(String fileName, String backupFileName) {
        OSS ossClient = null;
        try {
            ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
            // 检查文件是否存在
            if (!ossClient.doesObjectExist(bucket, fileName)) {
                if (StrUtil.isBlank(backupFileName)) {
                    return null;
                }
                fileName = backupFileName;
                if (!ossClient.doesObjectExist(bucket, fileName)) {
                    return null;
                }
            }
            /* 检查文件是否修改
            SimplifiedObjectMeta metadata = ossClient.getSimplifiedObjectMeta(bucket, fileName);
            long ts = metadata.getLastModified().getTime();
            */
            return ossClient.getObject(bucket, fileName);
        } catch (Exception e) {
            log.error("loadFile from Oss fail, fileName: {}, backupFileName: {}", fileName, Optional.ofNullable(backupFileName).orElse(""));
        } finally {
            if (ossClient != null) {
                try { ossClient.shutdown(); } catch (Exception ignore) {}
            }
        }
        return null;
    }

}
