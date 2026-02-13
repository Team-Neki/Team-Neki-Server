package com.yapp2app.media.application.usecase

import com.yapp2app.common.annotation.UseCase
import com.yapp2app.media.application.command.GetMediasCommand
import com.yapp2app.media.application.port.MediaBinaryCachePort
import com.yapp2app.media.application.port.MediaRepositoryPort
import com.yapp2app.media.application.port.MediaStoragePort
import com.yapp2app.media.application.result.GetMediasResult

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
        val medias = mediaRepository.getActiveMedias(command.ownerId, command.mediaIds)

        val mediaInfos = medias.map { it ->
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
