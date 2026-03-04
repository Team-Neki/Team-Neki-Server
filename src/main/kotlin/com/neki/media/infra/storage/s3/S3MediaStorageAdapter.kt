package com.neki.media.infra.storage.s3

import com.neki.media.contract.UploadTicket
import com.neki.media.application.dto.MediaRef
import com.neki.media.application.port.MediaStoragePort
import com.neki.media.domain.MediaType
import software.amazon.awssdk.core.ResponseBytes
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import software.amazon.awssdk.services.s3.model.GetObjectResponse
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request
import software.amazon.awssdk.services.s3.model.NoSuchKeyException
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import software.amazon.awssdk.services.s3.model.S3Exception
import software.amazon.awssdk.services.s3.presigner.S3Presigner
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest
import java.time.Instant

/**
 * fileName       : S3MediaStorage
 * author         : koo
 * date           : 2025. 12. 19. 오전 2:40
 * description    : 이미지 업로드(MediaStorage) S3 구현체
 */
class S3MediaStorageAdapter(
    private val s3Client: S3Client,
    private val s3Presigner: S3Presigner,
    private val props: S3Properties,
) : MediaStoragePort {

    override fun deleteByKey(key: String) {
        s3Client.deleteObject {
            it.bucket(props.bucket).key(key)
        }
    }

    override fun findByKey(key: String): String = "${props.baseUrl}/$key"

    override fun fetchBinaryByKey(key: String): ByteArray {
        val getObjectRequest = GetObjectRequest.builder()
            .bucket(props.bucket)
            .key(key)
            .build()

        val responseBytes: ResponseBytes<GetObjectResponse> =
            s3Client.getObjectAsBytes(getObjectRequest)
        return responseBytes.asByteArray()
    }

    override fun findAll(prefix: String): List<MediaRef> {
        val request = ListObjectsV2Request.builder()
            .bucket(props.bucket)
            .prefix(prefix)
            .build()

        val response = s3Client.listObjectsV2(request)

        return response.contents()
            .map { s3Object ->
                MediaRef(
                    key = s3Object.key(),
                    url = "${props.baseUrl}/${s3Object.key()}",
                    type = MediaType.valueOf(s3Object.key().substringBefore("/").uppercase()),
                )
            }
    }

    override fun exists(key: String): Boolean = try {
        s3Client.headObject {
            it.bucket(props.bucket)
            it.key(key)
        }
        true
    } catch (_: NoSuchKeyException) {
        false
    } catch (e: S3Exception) {
        // 404 (Not Found)인 경우만 false, 나머지는 throw
        if (e.statusCode() == 404) {
            false
        } else {
            throw e
        }
    }

    override fun generateUploadTicket(key: String, contentType: String): UploadTicket {
        val putObjectRequest = PutObjectRequest.builder()
            .bucket(props.bucket)
            .key(key)
            .contentType(contentType)
            .build()

        val presignRequest = PutObjectPresignRequest.builder()
            .signatureDuration(props.presignedUrlExpiration)
            .putObjectRequest(putObjectRequest)
            .build()

        val presignedRequest = s3Presigner.presignPutObject(presignRequest)

        return UploadTicket(
            url = presignedRequest.url().toString(),
            method = "PUT",
            expiresAt = Instant.now().plus(props.presignedUrlExpiration),
            contentType = contentType,
        )
    }
}
