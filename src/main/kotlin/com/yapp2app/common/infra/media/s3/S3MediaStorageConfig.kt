package com.yapp2app.common.infra.media.s3

import com.yapp2app.common.media.MediaStorage
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.S3Configuration
import software.amazon.awssdk.services.s3.presigner.S3Presigner
import java.net.URI

/**
 * fileName       : S3Config
 * author         : koo
 * date           : 2025. 12. 19. 오전 2:40
 * description    : S3 설정
 */
@Configuration
class S3MediaStorageConfig(private val props: S3Properties) {

    @Bean
    fun s3Client(): S3Client {
        val credentials = AwsBasicCredentials.create(
            props.accessKey,
            props.secretKey,
        )

        val builder = S3Client.builder()
            .region(Region.of(props.region))
            .credentialsProvider(StaticCredentialsProvider.create(credentials))

        // LocalStack을 위한 엔드포인트 설정
        props.endpoint?.let {
            builder.endpointOverride(URI.create(it))
                .forcePathStyle(true) // LocalStack은 path-style 필수
        }

        return builder.build()
    }

    @Bean
    fun s3Presigner(): S3Presigner {
        val credentials = AwsBasicCredentials.create(
            props.accessKey,
            props.secretKey,
        )

        val builder = S3Presigner.builder()
            .region(Region.of(props.region))
            .credentialsProvider(StaticCredentialsProvider.create(credentials))

        // LocalStack을 위한 엔드포인트 설정
        props.endpoint?.let {
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
    fun mediaStorage(s3Client: S3Client, s3Presigner: S3Presigner): MediaStorage = S3MediaStorage(
        s3Client = s3Client,
        s3Presigner = s3Presigner,
        bucketName = props.bucket,
        baseUrl = props.baseUrl,
    )
}
