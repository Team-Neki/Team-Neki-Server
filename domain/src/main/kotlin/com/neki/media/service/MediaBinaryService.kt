package com.neki.media.service

import com.neki.media.DistributedLock
import com.neki.media.MediaBinaryCache
import com.neki.media.MediaStorage
import com.neki.media.dto.MediaQuery
import com.neki.media.models.Media
import com.neki.media.models.MediaType
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.Duration

/**
 * fileName       : MediaBinaryService
 * author         : koo
 * date           : 2026. 8. 3. 오전 1:14
 * description    : 미디어 바이너리 조회 도메인 서비스 (캐시 우선, cache miss 시 스토리지 조회)
 */
@Component
class MediaBinaryService(
    private val binaryCache: MediaBinaryCache,
    private val mediaStorage: MediaStorage,
    private val distributedLock: DistributedLock,
) {
    private val log: Logger = LoggerFactory.getLogger(javaClass)

    fun evict(media: Media) = binaryCache.evict(media.storageKey)

    /**
     * 미디어 바이너리 조회
     */
    fun getBinary(media: Media): ByteArray = getBinary(media.storageKey, media.mediaType)

    /**
     * object key로 바이너리 조회 (key prefix로 미디어 타입 판별)
     */
    fun getBinaryByKey(query: MediaQuery.GetImageByKey): ByteArray =
        getBinary(query.objectKey, MediaType.fromObjectKey(query.objectKey))

    /**
     * 캐싱 대상이면 cache-aside, cache miss 시 분산 락으로 중복 S3 호출 방지
     */
    private fun getBinary(objectKey: String, mediaType: MediaType?): ByteArray {
        // 캐싱 대상이 아닌 타입은 S3에서 직접 조회
        val cacheTtl: Duration = mediaType?.cacheTtl ?: return mediaStorage.fetchBinaryByKey(objectKey)

        // cache를 조회하고 있다면 바로 반환
        binaryCache.get(objectKey)?.let {
            log.debug("[GetImage] Cache hit for key: $objectKey")
            return it
        }

        // cache가 없다면 lock을 획득하여 S3에서 데이터를 가져오고 캐싱
        return distributedLock.executeWithLock(objectKey) {
            fetchAndCache(objectKey, cacheTtl)
        }
            ?: binaryCache.get(objectKey) // 락 홀더가 채운 캐시 재확인
            ?: mediaStorage.fetchBinaryByKey(objectKey) // 락 미획득 + 캐시도 비어있으면 S3 직접 조회
    }

    private fun fetchAndCache(objectKey: String, cacheTtl: Duration): ByteArray {
        // lock 획득 후에도 캐시가 채워졌는지 재확인
        binaryCache.get(objectKey)?.let {
            log.debug("[GetImage] Cache hit after lock acquisition for key: $objectKey")
            return it
        }

        // S3에서 이미지 바이너리 조회 후 캐싱
        log.debug("[GetImage] Fetching from S3 for key: $objectKey")
        return mediaStorage.fetchBinaryByKey(objectKey).also {
            binaryCache.put(objectKey, it, cacheTtl)
        }
    }
}
