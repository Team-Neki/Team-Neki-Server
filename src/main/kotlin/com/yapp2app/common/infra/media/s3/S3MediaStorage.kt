package com.yapp2app.common.infra.media.s3

import com.yapp2app.common.media.MediaRef
import com.yapp2app.common.media.MediaStorage
import com.yapp2app.common.media.MediaType
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import software.amazon.awssdk.services.s3.presigner.S3Presigner
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest
import java.time.Duration

/**
 * fileName       : S3MediaStorage
 * author         : koo
 * date           : 2025. 12. 19. 오전 2:40
 * description    : 이미지 업로드(MediaStorage) S3 구현체
 */
class S3MediaStorage(
    private val s3Client: S3Client,
    private val s3Presigner: S3Presigner,
    private val bucketName: String,
    private val baseUrl: String,
) : MediaStorage {

    override fun deleteByKey(key: String) {
        s3Client.deleteObject {
            it.bucket(bucketName).key(key)
        }
    }

    override fun findByKey(key: String): String = "$baseUrl/$key"

    override fun findAll(prefix: String): List<MediaRef> {
        val request = ListObjectsV2Request.builder()
            .bucket(bucketName)
            .prefix(prefix)
            .build()

        val response = s3Client.listObjectsV2(request)

        return response.contents()
            .map { s3Object ->
                MediaRef(
                    key = s3Object.key(),
                    url = "$baseUrl/${s3Object.key()}",
                    type = MediaType.valueOf(s3Object.key().substringBefore("/").uppercase()),
                )
            }
    }

    override fun generatePresignedUrl(key: String, contentType: String, expirationMinutes: Long): String {
        val putObjectRequest = PutObjectRequest.builder()
            .bucket(bucketName)
            .key(key)
            .contentType(contentType)
            .build()

        val presignRequest = PutObjectPresignRequest.builder()
            .signatureDuration(Duration.ofMinutes(expirationMinutes))
            .putObjectRequest(putObjectRequest)
            .build()

        val presignedRequest = s3Presigner.presignPutObject(presignRequest)

        return presignedRequest.url().toString()
    }
}
