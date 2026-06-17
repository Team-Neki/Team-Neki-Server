package com.neki.media.application.usecase

import com.neki.common.annotation.UseCase
import com.neki.media.application.command.GetMediaStorageInfosCommand
import com.neki.media.application.port.MediaRepositoryPort
import com.neki.media.application.result.GetMediaStorageInfosResult
import com.neki.media.domain.entity.Media

/**
 * fileName       : GetMediaStorageInfosUseCase
 * author         : koo
 * date           : 2026. 1. 21.
 * description    : media storage key 정보 조회 usecase (이미지 URL 생성용)
 */
@UseCase
class GetMediaStorageInfosUseCase(private val mediaRepository: MediaRepositoryPort) {

    fun execute(command: GetMediaStorageInfosCommand): GetMediaStorageInfosResult {
        val medias: List<Media> = command.ownerId?.let {
            mediaRepository.getActiveMedias(it, command.mediaIds)
        } ?: mediaRepository.getActiveMedias(command.mediaIds)

        val storageInfos: List<GetMediaStorageInfosResult.StorageInfo> = medias.map {
            GetMediaStorageInfosResult.StorageInfo(
                mediaId = it.id!!,
                storageKey = it.storageKey,
                contentType = it.contentType,
                width = it.width,
                height = it.height,
            )
        }

        return GetMediaStorageInfosResult(storageInfos)
    }
}
