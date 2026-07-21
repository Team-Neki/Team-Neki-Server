package com.neki.media.infra.storage.s3

import com.neki.media.application.port.MediaStoragePort
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.presigner.S3Presigner

/**
 * fileName       : S3MediaStorageAdapterConfig
 * author         : koo
 * description    : S3 연결 빈(S3Client/S3Presigner)을 사용해 MediaStoragePort 어댑터를 와이어링.
 *                  연결설정(S3MediaStorageConfig)은 :modules:s3 에 위치.
 */
@Profile("!test")
@Configuration
class S3MediaStorageAdapterConfig(private val s3Props: S3Properties) {

    @Bean
    fun mediaStorage(s3Client: S3Client, s3Presigner: S3Presigner): MediaStoragePort = S3MediaStorageAdapter(
        s3Client = s3Client,
        s3Presigner = s3Presigner,
        props = s3Props,
    )
}
