package com.yapp2app.media.infra.s3

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * fileName       : S3Properties
 * author         : koo
 * date           : 2025. 12. 19. 오전 2:40
 * description    : S3 관련 설정
 */
@ConfigurationProperties(prefix = "aws.s3")
data class S3Properties(
    val accessKey: String,
    val secretKey: String,
    val region: String,
    val bucket: String,
    val endpoint: String? = null,
    val baseUrl: String = "",
)
