package com.kato.pro.s3.aws;

import cn.hutool.core.util.IdUtil;
import com.amazonaws.services.s3.model.Bucket;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.kato.pro.s3.config.S3Template;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.util.*;

@Slf4j
@RequestMapping("/aws")
public class AwsEndpoint {

    private final S3Template s3Template;

    public AwsEndpoint(S3Template s3Template) {
        this.s3Template = s3Template;
    }


    /**
     * Bucket Endpoints
     */
    @SneakyThrows
    @PostMapping("/bucket/{bucketName}")
    public Bucket createBucket(@PathVariable String bucketName) {

        s3Template.createBucket(bucketName);
        return s3Template.getBucketByName(bucketName).get();

    }

    @SneakyThrows
    @GetMapping("/bucket")
    public List<Bucket> getBuckets() {
        return s3Template.getAllBucket();
    }

    @SneakyThrows
    @GetMapping("/bucket/{bucketName}")
    public Bucket getBucket(@PathVariable String bucketName) {
        return s3Template.getBucketByName(bucketName).orElseThrow(() -> new IllegalArgumentException("Bucket Name not found!"));
    }

    @SneakyThrows
    @DeleteMapping("/bucket/{bucketName}")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void deleteBucket(@PathVariable String bucketName) {
        s3Template.removeBucket(bucketName);
    }

    /**
     * Object Endpoints
     */
    @SneakyThrows
    @PostMapping("/object/{bucketName}")
    public S3Object createObject(@RequestBody MultipartFile object, @PathVariable String bucketName) {
        String name = object.getOriginalFilename();
        s3Template.uploadObject(bucketName, name, object.getInputStream(), object.getSize(), object.getContentType());
        return s3Template.getObject(bucketName, name);

    }

    @SneakyThrows
    @PostMapping("/object/{bucketName}/{objectName}")
    public S3Object createObject(@RequestBody MultipartFile object, @PathVariable String bucketName,
                                 @PathVariable String objectName) {
        s3Template.uploadObject(bucketName, objectName, object.getInputStream(), object.getSize(), object.getContentType());
        return s3Template.getObject(bucketName, objectName);

    }

    @SneakyThrows
    @GetMapping("/object/{bucketName}/{objectName}")
    public List<S3ObjectSummary> filterObject(@PathVariable String bucketName, @PathVariable String objectName) {
        return s3Template.getObjectsByPrefix(bucketName, objectName);
    }

    @SneakyThrows
    @GetMapping("/object/{bucketName}/{objectName}/{expires}")
    public Map<String, Object> getObject(@PathVariable String bucketName, @PathVariable String objectName,
                                         @PathVariable Integer expires) {
        Map<String, Object> responseBody = new HashMap<>(8);
        responseBody.put("bucket", bucketName);
        responseBody.put("object", objectName);
        responseBody.put("url", s3Template.getObjectUrl(bucketName, objectName, expires));
        responseBody.put("expires", expires);
        return responseBody;
    }

    @SneakyThrows
    @ResponseStatus(HttpStatus.ACCEPTED)
    @DeleteMapping("/object/{bucketName}/{objectName}/")
    public void deleteObject(@PathVariable String bucketName, @PathVariable String objectName) {
        s3Template.removeObject(bucketName, objectName);
    }

    @SneakyThrows
    @GetMapping("/object/{bucketName}/{objectName}")
    public void downloadObject(@PathVariable String bucketName, @PathVariable String objectName, HttpServletResponse response) {
        S3Object s3Object = s3Template.getObject(bucketName, objectName);
        if (Objects.isNull(s3Object)) {
            throw new IllegalArgumentException("file not exist");
        }
        response.addHeader("Content-Disposition", "attachment; filename=" + IdUtil.fastSimpleUUID() + ".csv");
        byte[] buffer = new byte[1024];
        int length;
        try (S3ObjectInputStream inputStream = s3Object.getObjectContent();
             ServletOutputStream out = response.getOutputStream() ){
            while ( (length = inputStream.read( buffer )) != -1 ) {
                out.write( buffer, 0, length );
            }
            inputStream.close();
            out.flush();
        } catch (Exception e) {
            log.info( "downloadS3File fail, bucket: {}, objectName: {}", bucketName, objectName, e );
        }
    }
}