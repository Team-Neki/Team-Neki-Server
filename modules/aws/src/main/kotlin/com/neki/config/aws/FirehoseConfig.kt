package com.neki.config.aws

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.firehose.FirehoseClient

/**
 * fileName       : FirehoseConfig
 * author         : koo
 * date           : 2026. 10. 2.
 * description    : Kinesis Firehose 연결 설정. LocalStack 은 s3 만 띄우므로 local 은 fake 어댑터를 쓴다.
 *                  S3 와 같은 yapp IAM 사용자 키를 쓴다 (Firehose 권한은 BACKEND-125 에서 부여).
 */
@Profile("!test & !local")
@Configuration
class FirehoseConfig(private val s3Props: S3Properties) {

    @Bean
    fun firehoseClient(): FirehoseClient = FirehoseClient.builder()
        .region(Region.of(s3Props.region))
        .credentialsProvider(
            StaticCredentialsProvider.create(AwsBasicCredentials.create(s3Props.accessKey, s3Props.secretKey)),
        )
        .build()
}
