package com.neki.config.aws

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.firehose.FirehoseClient
import java.time.Duration

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
        // 로그 API 는 다른 API 와 Tomcat 스레드를 공유한다. Firehose 지연 시 기본값(소켓 30초 x 재시도)으로 붙잡혀 있지 않고
        // 빨리 LOG_SEND_FAILED 로 끊는다. 시도 3초는 요청 최대 4MiB(base64 후 약 5.3MB)를 클러스터 밖에서 올리는 시간을 감안한 값
        .overrideConfiguration {
            it.apiCallAttemptTimeout(Duration.ofSeconds(3))
                .apiCallTimeout(Duration.ofSeconds(5))
        }
        .build()
}
