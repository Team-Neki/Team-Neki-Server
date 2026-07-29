package com.neki.media.application.usecase

import com.neki.common.annotation.UseCase
import com.neki.media.application.dto.MediaQuery
import com.neki.media.application.dto.MediaResult
import com.neki.media.application.port.MediaRepositoryPort
import com.neki.media.domain.entity.Media

/**
 * fileName       : GetMediaStorageInfosUseCase
 * author         : koo
 * date           : 2026. 1. 21.
 * description    : media storage key 정보 조회 usecase (이미지 URL 생성용)
 */
@UseCase
class GetMediaStorageInfosUseCase(private val mediaRepository: MediaRepositoryPort) {

    fun execute(query: MediaQuery.GetMediaStorageInfos): MediaResult.GetMediaStorageInfos {
        val medias: List<Media> = query.ownerId?.let {
            mediaRepository.getActiveMedias(it, query.mediaIds)
        } ?: mediaRepository.getActiveMedias(query.mediaIds)

        val storageInfos: List<MediaResult.GetMediaStorageInfos.Item> = medias.map {
            MediaResult.GetMediaStorageInfos.Item(
                mediaId = it.id!!,
                storageKey = it.storageKey,
                contentType = it.contentType,
                width = it.width,
                height = it.height,
            )
        }

        return MediaResult.GetMediaStorageInfos(storageInfos)
    }
}
