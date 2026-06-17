package com.neki.media.infra.storage.s3

import com.neki.media.application.port.MediaStoragePort
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.S3Configuration
import software.amazon.awssdk.services.s3.presigner.S3Presigner
import java.net.URI

/**
 * fileName       : MediaStorageConfig
 * author         : koo
 * date           : 2025. 12. 19. 오전 2:40
 * description    : ObjectStorage 설정
 */
@Profile("!test")
@Configuration
class S3MediaStorageConfig(private val s3Props: S3Properties) {

    @Bean
    fun s3Client(): S3Client {
        val credentials = AwsBasicCredentials.create(
            s3Props.accessKey,
            s3Props.secretKey,
        )

        val builder = S3Client.builder()
            .region(Region.of(s3Props.region))
            .credentialsProvider(StaticCredentialsProvider.create(credentials))

        // LocalStack을 위한 엔드포인트 설정
        s3Props.endpoint?.let {
            builder.endpointOverride(URI.create(it))
                .forcePathStyle(true) // LocalStack은 path-style 필수
        }

        return builder.build()
    }

    @Bean
    fun s3Presigner(): S3Presigner {
        val credentials = AwsBasicCredentials.create(
            s3Props.accessKey,
            s3Props.secretKey,
        )

        val builder = S3Presigner.builder()
            .region(Region.of(s3Props.region))
            .credentialsProvider(StaticCredentialsProvider.create(credentials))

        // LocalStack을 위한 엔드포인트 설정
        s3Props.endpoint?.let {
            builder.endpointOverride(URI.create(it))
                .serviceConfiguration(
                    S3Configuration.builder()
                        .pathStyleAccessEnabled(true) // LocalStack은 path-style 필수
                        .build(),
                )
        }

        return builder.build()
    }

    @Bean
    fun mediaStorage(s3Client: S3Client, s3Presigner: S3Presigner): MediaStoragePort = S3MediaStorageAdapter(
        s3Client = s3Client,
        s3Presigner = s3Presigner,
        props = s3Props,
    )
}
