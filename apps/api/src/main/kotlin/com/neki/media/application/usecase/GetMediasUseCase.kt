package com.neki.media.application.usecase

import com.neki.common.annotation.UseCase
import com.neki.media.application.dto.MediaQuery
import com.neki.media.application.dto.MediaResult
import com.neki.media.application.port.MediaBinaryCachePort
import com.neki.media.application.port.MediaRepositoryPort
import com.neki.media.application.port.MediaStoragePort
import com.neki.media.entity.Media

/**
 * fileName       : GetMediasUseCase
 * author         : koo
 * date           : 2026. 1. 3. 오전 3:39
 * description    : media 정보 조회 usecase
 */
@UseCase
class GetMediasUseCase(
    private val mediaRepository: MediaRepositoryPort,
    private val mediaStorage: MediaStoragePort,

    private val cache: MediaBinaryCachePort,
) {

    fun execute(query: MediaQuery.GetMedias): MediaResult.GetMedias {
        val medias: List<Media> = mediaRepository.getActiveMedias(query.ownerId, query.mediaIds)

        val mediaInfos: List<MediaResult.GetMedias.Item> = medias.map { it ->
            val storageKey = it.storageKey

            val binaryData = if (it.mediaType.isCacheable) {
                cache.get(storageKey)
                    ?: mediaStorage.fetchBinaryByKey(storageKey).also { data ->
                        cache.put(storageKey, data, it.mediaType.cacheTtl!!)
                    }
            } else {
                mediaStorage.fetchBinaryByKey(storageKey)
            }

            MediaResult.GetMedias.Item(
                mediaId = it.id!!,
                binaryData = binaryData,
                contentType = it.contentType,
            )
        }

        return MediaResult.GetMedias(mediaInfos)
    }
}
