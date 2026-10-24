package com.neki.domain.media.infra.storage.s3

import com.neki.config.aws.S3Properties
import com.neki.domain.media.external.MediaStorage
import com.neki.domain.media.models.MediaRef
import com.neki.domain.media.models.MediaStorageUploadTicket
import com.neki.domain.media.models.MediaType
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
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
    private val meterRegistry: MeterRegistry,
) : MediaStorage {

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

        // http.server.requests 는 클라이언트로 바이트를 쓰는 시간까지 포함하므로 S3 조회 구간만 따로 잰다.
        // 실패한 조회는 기록하지 않는다 (NoSuchKey 처럼 빠르게 끝나는 실패가 분포를 끌어내리지 않게)
        val sample: Timer.Sample = Timer.start(meterRegistry)
        val responseBytes: ResponseBytes<GetObjectResponse> =
            s3Client.getObjectAsBytes(getObjectRequest)
        sample.stop(fetchTimer(key))
        return responseBytes.asByteArray()
    }

    private fun fetchTimer(key: String): Timer = Timer.builder(FETCH_METRIC)
        .tag("type", MediaType.fromObjectKey(key)?.prefix ?: "unknown")
        .publishPercentileHistogram()
        .register(meterRegistry)

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

    override fun generateUploadTicket(key: String, contentType: String): MediaStorageUploadTicket {
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

        return MediaStorageUploadTicket(
            url = presignedRequest.url().toString(),
            method = "PUT",
            expiresAt = Instant.now().plus(props.presignedUrlExpiration),
            contentType = contentType,
        )
    }

    companion object {
        private const val FETCH_METRIC = "media.storage.fetch"
    }
}
