package com.yapp2app.common.infra.media.s3

import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.CORSConfiguration
import software.amazon.awssdk.services.s3.model.CORSRule
import software.amazon.awssdk.services.s3.model.HeadBucketRequest
import software.amazon.awssdk.services.s3.model.NoSuchBucketException
import software.amazon.awssdk.services.s3.model.PutBucketCorsRequest

/**
 * fileName       : S3BucketInitializer
 * author         : koo
 * date           : 2025. 12. 22.
 * description    : S3 버킷 초기화 (CORS 설정 등)
 */
@Component
@Profile("local") // local 환경에서만 실행
class S3BucketInitializer(private val s3Client: S3Client, private val props: S3Properties) {

    private val log = LoggerFactory.getLogger(javaClass)

    @PostConstruct
    fun initializeBucket() {
        // LocalStack 환경이 아니면 실행하지 않음
        if (props.endpoint == null) {
            log.info("Skipping S3 bucket initialization (not LocalStack environment)")
            return
        }

        try {
            // 버킷 존재 여부 확인
            checkBucketExists()

            // CORS 설정 적용
            configureCors()

            log.info("S3 bucket '${props.bucket}' initialized successfully with CORS configuration")
        } catch (e: Exception) {
            log.error("Failed to initialize S3 bucket: ${e.message}", e)
        }
    }

    private fun checkBucketExists() {
        try {
            s3Client.headBucket(HeadBucketRequest.builder().bucket(props.bucket).build())
            log.info("Bucket '${props.bucket}' exists")
        } catch (e: NoSuchBucketException) {
            log.warn("Bucket '${props.bucket}' does not exist. Please create it first.")
            throw e
        }
    }

    private fun configureCors() {
        val corsRule = CORSRule.builder()
            .allowedOrigins(
                "http://localhost:3000",
                "http://localhost:5173",
                "http://localhost:63342",
                "http://127.0.0.1:3000",
                "http://127.0.0.1:5173",
                "http://127.0.0.1:63342",
            )
            .allowedMethods("GET", "PUT", "POST", "DELETE", "HEAD")
            .allowedHeaders("*")
            .exposeHeaders(
                "ETag",
                "x-amz-server-side-encryption",
                "x-amz-request-id",
                "x-amz-id-2",
            )
            .maxAgeSeconds(3600)
            .build()

        val corsConfiguration = CORSConfiguration.builder()
            .corsRules(corsRule)
            .build()

        s3Client.putBucketCors(
            PutBucketCorsRequest.builder()
                .bucket(props.bucket)
                .corsConfiguration(corsConfiguration)
                .build(),
        )

        log.info("CORS configuration applied to bucket '${props.bucket}'")
    }
}
