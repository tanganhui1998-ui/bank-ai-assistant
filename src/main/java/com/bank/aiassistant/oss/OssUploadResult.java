package com.bank.aiassistant.oss;

import lombok.Builder;

@Builder
public record OssUploadResult(
        String bucketName,
        String objectKey,
        String eTag,
        String accessUrl
) {
}
