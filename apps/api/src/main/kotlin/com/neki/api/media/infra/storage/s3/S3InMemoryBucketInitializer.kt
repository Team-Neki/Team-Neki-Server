package com.neki.api.media.infra.storage.s3

import com.neki.api.common.properties.AppProperties
import com.neki.config.aws.S3Properties
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
 * description    : 로컬 테스트용 S3 버킷 초기화 (CORS 설정 등)
 *                  CORS 허용 origin은 application.yaml의 app.cors.allowed-origins에서 관리 (SSOT)
 */
@Component
@Profile("local") // local 환경에서만 실행
class S3InMemoryBucketInitializer(
    private val s3Client: S3Client,
    private val s3Props: S3Properties,
    private val appProps: AppProperties,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @PostConstruct
    fun initializeBucket() {
        // LocalStack 환경이 아니면 실행하지 않음
        if (s3Props.endpoint == null) {
            log.info("Skipping S3 bucket initialization (not LocalStack environment)")
            return
        }

        try {
            checkBucketExists()

            configureCors()

            log.info("S3 bucket '${s3Props.bucket}' initialized successfully with CORS configuration")
        } catch (e: Exception) {
            log.error("Failed to initialize S3 bucket: ${e.message}", e)
        }
    }

    private fun checkBucketExists() {
        try {
            s3Client.headBucket(HeadBucketRequest.builder().bucket(s3Props.bucket).build())
            log.info("Bucket '${s3Props.bucket}' exists")
        } catch (e: NoSuchBucketException) {
            log.warn("Bucket '${s3Props.bucket}' does not exist. Please create it first.")
            throw e
        }
    }

    private fun configureCors() {
        val allowedOrigins = appProps.cors.allowedOrigins

        if (allowedOrigins.isEmpty()) {
            log.warn("No CORS origins configured in app.cors.allowed-origins")
            return
        }

        val corsRule = CORSRule.builder()
            .allowedOrigins(allowedOrigins)
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
                .bucket(s3Props.bucket)
                .corsConfiguration(corsConfiguration)
                .build(),
        )

        log.info("CORS configuration applied to bucket '${s3Props.bucket}' with origins: $allowedOrigins")
    }
}
