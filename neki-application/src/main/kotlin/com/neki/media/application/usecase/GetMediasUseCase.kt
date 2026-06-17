package com.neki.media.application.usecase

import com.neki.common.annotation.UseCase
import com.neki.media.application.command.GetMediasCommand
import com.neki.media.application.port.MediaBinaryCachePort
import com.neki.media.application.port.MediaRepositoryPort
import com.neki.media.application.port.MediaStoragePort
import com.neki.media.application.result.GetMediasResult
import com.neki.media.domain.entity.Media

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

    fun execute(command: GetMediasCommand): GetMediasResult {
        val medias: List<Media> = mediaRepository.getActiveMedias(command.ownerId, command.mediaIds)

        val mediaInfos: List<GetMediasResult.MediaInfo> = medias.map { it ->
            val storageKey = it.storageKey

            val binaryData = if (it.mediaType.isCacheable) {
                cache.get(storageKey)
                    ?: mediaStorage.fetchBinaryByKey(storageKey).also { data ->
                        cache.put(storageKey, data, it.mediaType.cacheTtl!!)
                    }
            } else {
                mediaStorage.fetchBinaryByKey(storageKey)
            }

            GetMediasResult.MediaInfo(
                mediaId = it.id!!,
                binaryData = binaryData,
                contentType = it.contentType,
            )
        }

        return GetMediasResult(mediaInfos)
    }
}
