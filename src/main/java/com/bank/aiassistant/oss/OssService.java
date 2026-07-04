package com.bank.aiassistant.oss;

import com.aliyun.oss.OSS;
import com.aliyun.oss.model.OSSObject;
import com.aliyun.oss.model.PutObjectResult;
import com.bank.aiassistant.config.OssProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.net.URL;
import java.time.Duration;
import java.util.Date;

@Service
@RequiredArgsConstructor
public class OssService {

    private static final Duration DEFAULT_URL_TTL = Duration.ofMinutes(5);

    private final OSS ossClient;
    private final OssProperties ossProperties;

    public OssUploadResult upload(String objectKey, InputStream inputStream) {
        PutObjectResult result = ossClient.putObject(ossProperties.getBucketName(), objectKey, inputStream);
        return OssUploadResult.builder()
                .bucketName(ossProperties.getBucketName())
                .objectKey(objectKey)
                .eTag(result.getETag())
                .accessUrl(buildAccessUrl(objectKey))
                .build();
    }

    public InputStream download(String objectKey) {
        OSSObject ossObject = ossClient.getObject(ossProperties.getBucketName(), objectKey);
        return ossObject.getObjectContent();
    }

    public URL generateTemporaryUrl(String objectKey) {
        Date expiration = new Date(System.currentTimeMillis() + DEFAULT_URL_TTL.toMillis());
        return ossClient.generatePresignedUrl(ossProperties.getBucketName(), objectKey, expiration);
    }

    public void delete(String objectKey) {
        ossClient.deleteObject(ossProperties.getBucketName(), objectKey);
    }

    public boolean exists(String objectKey) {
        return ossClient.doesObjectExist(ossProperties.getBucketName(), objectKey);
    }

    private String buildAccessUrl(String objectKey) {
        String prefix = ossProperties.getPublicUrlPrefix();
        if (prefix == null || prefix.isBlank()) {
            return null;
        }
        return prefix.endsWith("/") ? prefix + objectKey : prefix + "/" + objectKey;
    }
}
