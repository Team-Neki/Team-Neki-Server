package com.neki.media.application.usecase

import com.neki.common.annotation.UseCase
import com.neki.common.code.ResultCode
import com.neki.common.exception.BusinessException
import com.neki.media.application.command.GetImageByKeyCommand
import com.neki.media.application.port.DistributedLockPort
import com.neki.media.application.port.MediaBinaryCachePort
import com.neki.media.application.port.MediaStoragePort
import com.neki.media.application.result.GetImageByKeyResult
import com.neki.media.domain.MediaType
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.Duration

/**
 * fileName       : GetImageByKeyUseCase
 * author         : koo
 * date           : 2026. 1. 21.
 * description    : object key로 이미지 바이너리 조회 (캐시 우선, cache miss 시 S3 조회)
 *
 * Cache stampede 방지:
 * - Cache hit: 즉시 반환
 * - Cache miss: 분산 락으로 중복 S3 호출 방지
 * - 캐싱 대상이 아닌 MediaType은 S3 직접 조회
 */
@UseCase
class GetImageByKeyUseCase(
    private val mediaStorage: MediaStoragePort,
    private val cache: MediaBinaryCachePort,
    private val distributedLock: DistributedLockPort,
) {
    private val log: Logger = LoggerFactory.getLogger(javaClass)

    fun execute(command: GetImageByKeyCommand): GetImageByKeyResult {
        val objectKey = command.objectKey
        val contentType: String = resolveContentType(objectKey)
        val mediaType: MediaType? = MediaType.fromObjectKey(objectKey)

        // 캐싱 대상이 아닌 타입은 S3에서 직접 조회
        if (mediaType == null || !mediaType.isCacheable) {
            val binaryData: ByteArray = mediaStorage.fetchBinaryByKey(objectKey)
            return GetImageByKeyResult(binaryData, contentType)
        }

        // cache를 조회하고 있다면 바로 반환
        val cachedData: ByteArray? = cache.get(objectKey)
        if (cachedData != null) {
            log.debug("[GetImage] Cache hit for key: $objectKey")
            return GetImageByKeyResult(cachedData, contentType)
        }

        // cache가 없다면 lock을 획득하여 S3에서 데이터를 가져오고 캐싱
        val cacheTtl = mediaType.cacheTtl!!
        val binaryData: ByteArray =
            distributedLock.executeWithLock(
                key = objectKey,
                ttl = Duration.ofSeconds(10),
            ) {
                fetchAndCache(objectKey, cacheTtl)
            }
                ?: cache.get(objectKey) // 락 홀더가 채운 캐시 재확인
                ?: throw BusinessException(ResultCode.ERROR)

        return GetImageByKeyResult(binaryData, contentType)
    }

    private fun fetchAndCache(objectKey: String, cacheTtl: Duration): ByteArray {
        // lock 획득 후에도 캐시가 채워졌는지 재확인
        cache.get(objectKey)?.let {
            log.debug("[GetImage] Cache hit after lock acquisition for key: $objectKey")
            return it
        }

        // S3에서 이미지 바이너리 조회 후 캐싱
        log.debug("[GetImage] Fetching from S3 for key: $objectKey")
        return mediaStorage.fetchBinaryByKey(objectKey).also {
            cache.put(objectKey, it, cacheTtl)
        }
    }

    private fun resolveContentType(objectKey: String): String {
        val extension: String = objectKey.substringAfterLast('.', "").lowercase()
        return EXTENSION_TO_CONTENT_TYPE[extension] ?: DEFAULT_CONTENT_TYPE
    }

    companion object {
        private const val DEFAULT_CONTENT_TYPE = "application/octet-stream"

        private val EXTENSION_TO_CONTENT_TYPE = mapOf(
            "jpg" to "image/jpeg",
            "jpeg" to "image/jpeg",
            "png" to "image/png",
            "gif" to "image/gif",
            "webp" to "image/webp",
            "heic" to "image/heic",
            "heif" to "image/heif",
            "svg" to "image/svg+xml",
            "bmp" to "image/bmp",
            "ico" to "image/x-icon",
        )
    }
}
