package com.yapp2app.media.application.usecase

import com.yapp2app.common.annotation.UseCase
import com.yapp2app.media.application.command.GetMediaStorageInfosCommand
import com.yapp2app.media.application.port.MediaRepositoryPort
import com.yapp2app.media.application.result.GetMediaStorageInfosResult

/**
 * fileName       : GetMediaStorageInfosUseCase
 * author         : koo
 * date           : 2026. 1. 21.
 * description    : media storage key 정보 조회 usecase (이미지 URL 생성용)
 */
@UseCase
class GetMediaStorageInfosUseCase(private val mediaRepository: MediaRepositoryPort) {

    fun execute(command: GetMediaStorageInfosCommand): GetMediaStorageInfosResult {
        val medias = command.ownerId?.let {
            mediaRepository.getActiveMedias(it, command.mediaIds)
        } ?: mediaRepository.getActiveMedias(command.mediaIds)

        val storageInfos = medias.map {
            GetMediaStorageInfosResult.StorageInfo(
                mediaId = it.id!!,
                storageKey = it.storageKey,
                contentType = it.contentType,
            )
        }

        return GetMediaStorageInfosResult(storageInfos)
    }
}
