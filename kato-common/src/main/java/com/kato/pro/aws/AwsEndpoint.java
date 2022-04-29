package com.kato.pro.aws;

import com.amazonaws.services.s3.model.Bucket;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@AllArgsConstructor
@RequestMapping("/aws")
public class AwsEndpoint {

    private final AwsTemplate template;

    /**
     * Bucket Endpoints
     */
    @SneakyThrows
    @PostMapping("/bucket/{bucketName}")
    public Bucket createBucket(@PathVariable String bucketName) {

        template.createBucket(bucketName);
        return template.getBucketByName(bucketName).get();

    }

    @SneakyThrows
    @GetMapping("/bucket")
    public List<Bucket> getBuckets() {
        return template.getAllBucket();
    }

    @SneakyThrows
    @GetMapping("/bucket/{bucketName}")
    public Bucket getBucket(@PathVariable String bucketName) {
        return template.getBucketByName(bucketName).orElseThrow(() -> new IllegalArgumentException("Bucket Name not found!"));
    }

    @SneakyThrows
    @DeleteMapping("/bucket/{bucketName}")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void deleteBucket(@PathVariable String bucketName) {
        template.removeBucket(bucketName);
    }

    /**
     * Object Endpoints
     */
    @SneakyThrows
    @PostMapping("/object/{bucketName}")
    public S3Object createObject(@RequestBody MultipartFile object, @PathVariable String bucketName) {
        String name = object.getOriginalFilename();
        template.uploadObject(bucketName, name, object.getInputStream(), object.getSize(), object.getContentType());
        return template.getObject(bucketName, name);

    }

    @SneakyThrows
    @PostMapping("/object/{bucketName}/{objectName}")
    public S3Object createObject(@RequestBody MultipartFile object, @PathVariable String bucketName,
                                 @PathVariable String objectName) {
        template.uploadObject(bucketName, objectName, object.getInputStream(), object.getSize(), object.getContentType());
        return template.getObject(bucketName, objectName);

    }

    @SneakyThrows
    @GetMapping("/object/{bucketName}/{objectName}")
    public List<S3ObjectSummary> filterObject(@PathVariable String bucketName, @PathVariable String objectName) {
        return template.getObjectsByPrefix(bucketName, objectName);
    }

    @SneakyThrows
    @GetMapping("/object/{bucketName}/{objectName}/{expires}")
    public Map<String, Object> getObject(@PathVariable String bucketName, @PathVariable String objectName,
                                         @PathVariable Integer expires) {
        Map<String, Object> responseBody = new HashMap<>(8);
        responseBody.put("bucket", bucketName);
        responseBody.put("object", objectName);
        responseBody.put("url", template.getObjectUrl(bucketName, objectName, expires));
        responseBody.put("expires", expires);
        return responseBody;
    }

    @SneakyThrows
    @ResponseStatus(HttpStatus.ACCEPTED)
    @DeleteMapping("/object/{bucketName}/{objectName}/")
    public void deleteObject(@PathVariable String bucketName, @PathVariable String objectName) {
        template.removeObject(bucketName, objectName);
    }

}