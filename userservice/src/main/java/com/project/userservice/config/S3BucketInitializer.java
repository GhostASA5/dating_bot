package com.project.userservice.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "aws.s3.initialize-bucket", havingValue = "true", matchIfMissing = true)
public class S3BucketInitializer implements ApplicationRunner {

    private final S3Client s3Client;

    @Value("${aws.s3.bucket}")
    private String bucket;

    @Override
    public void run(ApplicationArguments args) {
        try {
            s3Client.headBucket(HeadBucketRequest.builder().bucket(bucket).build());
            log.info("userservice.s3: bucket exists bucket={}", bucket);
        } catch (S3Exception e) {
            if (e.statusCode() == 404) {
                s3Client.createBucket(CreateBucketRequest.builder().bucket(bucket).build());
                log.info("userservice.s3: created bucket bucket={}", bucket);
            } else {
                log.error("userservice.s3: headBucket failed bucket={} status={} msg={}",
                        bucket, e.statusCode(), e.getMessage());
                throw e;
            }
        }
    }
}
